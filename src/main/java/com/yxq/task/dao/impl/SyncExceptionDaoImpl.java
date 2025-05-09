package com.yxq.task.dao.impl;

import com.yxq.task.dao.SyncExceptionDao;
import com.yxq.task.entity.SyncException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 同步异常DAO实现类
 */
@Slf4j
@Repository
public class SyncExceptionDaoImpl implements SyncExceptionDao {

    private final DataSource dataSource;

    public SyncExceptionDaoImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public int insert(SyncException exception) {
        String sql = "INSERT INTO cdc_sync_exception (task_id, table_name, error_message, error_time) " +
                    "VALUES (?, ?, ?, ?)";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, exception.getTaskId());
            stmt.setString(2, exception.getTableName());
            stmt.setString(3, exception.getErrorMessage());
            stmt.setTimestamp(4, new Timestamp(exception.getErrorTime().getTime()));
            
            int rows = stmt.executeUpdate();
            
            if (rows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        exception.setId(generatedKeys.getInt(1));
                    }
                }
            }
            
            return rows;
        } catch (SQLException e) {
            log.error("插入同步异常记录失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public int update(SyncException syncException) {
        return 0;
    }

    @Override
    public int deleteById(Integer id) {
        return 0;
    }

    @Override
    public int deleteByTaskId(Integer taskId) {
        String sql = "DELETE FROM cdc_sync_exception WHERE task_id = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, taskId);
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("删除任务异常记录失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public SyncException selectById(Integer id) {
        return null;
    }

    @Override
    public List<SyncException> selectByTaskId(Integer taskId) {
        return Collections.emptyList();
    }

    @Override
    public List<SyncException> selectByExecutionId(Integer executionId) {
        return Collections.emptyList();
    }

    @Override
    public List<SyncException> selectByTaskIdAndTableName(Integer taskId, String tableName) {
        String sql = "SELECT id, task_id, table_name, error_message, error_time, status, handle_time, handle_remark " +
                    "FROM cdc_sync_exception WHERE task_id = ? AND table_name = ? ORDER BY error_time DESC LIMIT 100";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, taskId);
            stmt.setString(2, tableName);
            
            List<SyncException> exceptions = new ArrayList<>();
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    SyncException exception = new SyncException();
                    exception.setId(rs.getInt("id"));
                    exception.setTaskId(rs.getInt("task_id"));
                    exception.setTableName(rs.getString("table_name"));
                    exception.setErrorMessage(rs.getString("error_message"));
                    exception.setErrorTime(rs.getTimestamp("error_time"));
                    // 可选字段
                    Timestamp handleTime = rs.getTimestamp("handle_time");
                    if (handleTime != null) {
                        exception.setHandleTime(handleTime);
                    }
                    exception.setHandleRemark(rs.getString("handle_remark"));
                    
                    exceptions.add(exception);
                }
            }
            
            return exceptions;
        } catch (SQLException e) {
            log.error("查询任务表异常记录失败: {}", e.getMessage(), e);
        }
        
        return Collections.emptyList();
    }

    @Override
    public int countByTaskId(Integer taskId) {
        String sql = "SELECT COUNT(*) FROM cdc_sync_exception WHERE task_id = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, taskId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            log.error("统计任务异常数量失败: {}", e.getMessage(), e);
        }
        
        return 0;
    }

    @Override
    public int countByTaskIdAndTableName(Integer taskId, String tableName) {
        String sql = "SELECT COUNT(*) FROM cdc_sync_exception WHERE task_id = ? AND table_name = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, taskId);
            stmt.setString(2, tableName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            log.error("统计任务表异常数量失败: {}", e.getMessage(), e);
        }
        
        return 0;
    }
} 