package com.yxq.task.dao.impl;

import com.yxq.task.dao.SyncTaskDao;
import com.yxq.task.entity.SyncTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 同步任务DAO实现类
 */
@Slf4j
@Repository
public class SyncTaskDaoImpl implements SyncTaskDao {

    private final DataSource dataSource;

    public SyncTaskDaoImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public int insert(SyncTask task) {
        String sql = "INSERT INTO cdc_sync_task (task_name, source_db_id, target_db_id, sync_type, tables, startup_options, status, create_time, update_time) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, task.getTaskName());
            stmt.setLong(2, task.getSourceDbId());
            stmt.setLong(3, task.getTargetDbId());
            stmt.setInt(4, task.getSyncType());
            stmt.setString(5, task.getTables());
            stmt.setString(6, task.getStartupOptions());
            stmt.setInt(7, task.getStatus());
            stmt.setTimestamp(8, new Timestamp(new Date().getTime()));
            stmt.setTimestamp(9, new Timestamp(new Date().getTime()));
            
            int rows = stmt.executeUpdate();
            
            if (rows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        task.setId(generatedKeys.getInt(1));
                    }
                }
            }
            
            return rows;
        } catch (SQLException e) {
            log.error("插入同步任务失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public int update(SyncTask task) {
        String sql = "UPDATE cdc_sync_task SET task_name = ?, source_db_id = ?, target_db_id = ?, sync_type = ?, " +
                     "tables = ?, startup_options = ?, status = ?, update_time = ? WHERE id = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, task.getTaskName());
            stmt.setLong(2, task.getSourceDbId());
            stmt.setLong(3, task.getTargetDbId());
            stmt.setInt(4, task.getSyncType());
            stmt.setString(5, task.getTables());
            stmt.setString(6, task.getStartupOptions());
            stmt.setInt(7, 0);
            stmt.setTimestamp(8, new Timestamp(new Date().getTime()));
            stmt.setLong(9, task.getId());
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("更新同步任务失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public int deleteById(Integer id) {
        String sql = "DELETE FROM cdc_sync_task WHERE id = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("删除同步任务失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public SyncTask selectById(Integer id) {
        String sql = "SELECT * FROM cdc_sync_task WHERE id = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSyncTask(rs);
                }
            }
        } catch (SQLException e) {
            log.error("查询同步任务失败: {}", e.getMessage(), e);
        }
        
        return null;
    }

    @Override
    public List<SyncTask> selectAll() {
        String sql = "SELECT * FROM cdc_sync_task ORDER BY id";
        List<SyncTask> taskList = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                taskList.add(mapResultSetToSyncTask(rs));
            }
        } catch (SQLException e) {
            log.error("查询所有同步任务失败: {}", e.getMessage(), e);
        }
        
        return taskList;
    }

    @Override
    public int updateStatus(Integer id, Integer status) {
        String sql = "UPDATE cdc_sync_task SET status = ?, update_time = ? WHERE id = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, status);
            stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            stmt.setLong(3, id);
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("更新同步任务状态失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public int updateTaskStartTime(Integer id, Timestamp startTime) {
        String sql = "UPDATE cdc_sync_task SET task_start_time = ?, update_time = ? WHERE id = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, startTime);
            stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            stmt.setLong(3, id);
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("更新任务开始时间失败: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * 将结果集映射为SyncTask对象
     */
    private SyncTask mapResultSetToSyncTask(ResultSet rs) throws SQLException {
        SyncTask task = new SyncTask();
        task.setId(rs.getInt("id"));
        task.setTaskName(rs.getString("task_name"));
        task.setSourceDbId(rs.getInt("source_db_id"));
        task.setTargetDbId(rs.getInt("target_db_id"));
        task.setSyncType(rs.getInt("sync_type"));
        task.setTables(rs.getString("tables"));
        task.setStartupOptions(rs.getString("startup_options"));
        task.setStatus(rs.getInt("status"));
        task.setTaskStartTime(rs.getTimestamp("task_start_time"));
        task.setCreateTime(rs.getTimestamp("create_time"));
        task.setUpdateTime(rs.getTimestamp("update_time"));
        return task;
    }
} 