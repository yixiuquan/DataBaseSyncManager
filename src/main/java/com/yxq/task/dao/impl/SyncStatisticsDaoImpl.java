package com.yxq.task.dao.impl;

import com.yxq.task.dao.SyncStatisticsDao;
import com.yxq.task.entity.SyncStatistics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 同步统计DAO实现类
 */
@Slf4j
@Repository
public class SyncStatisticsDaoImpl implements SyncStatisticsDao {

    private final DataSource dataSource;

    public SyncStatisticsDaoImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public int insert(SyncStatistics statistics) {
        String sql = "INSERT INTO cdc_table_statistics (task_id, execution_id, table_name, sync_count, " +
                     "total_count, exception_count, start_time, last_update_time, insert_count, update_count, " +
                     "delete_count, remark) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, statistics.getTaskId());
            stmt.setObject(2, statistics.getExecutionId());
            stmt.setString(3, statistics.getTableName());
            stmt.setInt(4, statistics.getSyncCount() != null ? statistics.getSyncCount() : 0);
            stmt.setInt(5, statistics.getTotalCount() != null ? statistics.getTotalCount() : 0);
            stmt.setInt(6, statistics.getExceptionCount() != null ? statistics.getExceptionCount() : 0);
            stmt.setTimestamp(7, statistics.getStartTime() != null ? 
                    new Timestamp(statistics.getStartTime().getTime()) : null);
            stmt.setTimestamp(8, statistics.getLastUpdateTime() != null ? 
                    new Timestamp(statistics.getLastUpdateTime().getTime()) : null);
            stmt.setInt(9, statistics.getInsertCount() != null ? statistics.getInsertCount() : 0);
            stmt.setInt(10, statistics.getUpdateCount() != null ? statistics.getUpdateCount() : 0);
            stmt.setInt(11, statistics.getDeleteCount() != null ? statistics.getDeleteCount() : 0);
            stmt.setString(12, statistics.getRemark());
            
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        statistics.setId(rs.getInt(1));
                    }
                }
            }
            
            return result;
        } catch (SQLException e) {
            log.error("插入同步统计数据失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public int update(SyncStatistics statistics) {
        String sql = "UPDATE cdc_table_statistics SET " +
                     "task_id = ?, execution_id = ?, table_name = ?, sync_count = ?, total_count = ?, " +
                     "exception_count = ?, start_time = ?, last_update_time = ?, insert_count = ?, " +
                     "update_count = ?, delete_count = ?, remark = ? " +
                     "WHERE id = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, statistics.getTaskId());
            stmt.setObject(2, statistics.getExecutionId());
            stmt.setString(3, statistics.getTableName());
            stmt.setInt(4, statistics.getSyncCount() != null ? statistics.getSyncCount() : 0);
            stmt.setInt(5, statistics.getTotalCount() != null ? statistics.getTotalCount() : 0);
            stmt.setInt(6, statistics.getExceptionCount() != null ? statistics.getExceptionCount() : 0);
            stmt.setTimestamp(7, statistics.getStartTime() != null ? 
                    new Timestamp(statistics.getStartTime().getTime()) : null);
            stmt.setTimestamp(8, statistics.getLastUpdateTime() != null ? 
                    new Timestamp(statistics.getLastUpdateTime().getTime()) : new Timestamp(new Date().getTime()));
            stmt.setInt(9, statistics.getInsertCount() != null ? statistics.getInsertCount() : 0);
            stmt.setInt(10, statistics.getUpdateCount() != null ? statistics.getUpdateCount() : 0);
            stmt.setInt(11, statistics.getDeleteCount() != null ? statistics.getDeleteCount() : 0);
            stmt.setString(12, statistics.getRemark());
            stmt.setInt(13, statistics.getId());
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("更新同步统计数据失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public int deleteById(Integer id) {
        String sql = "DELETE FROM cdc_table_statistics WHERE id = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("删除同步统计数据失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public int deleteByTaskId(Integer taskId) {
        String sql = "DELETE FROM cdc_table_statistics WHERE task_id = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, taskId);
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("删除任务关联的同步统计数据失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public SyncStatistics selectById(Integer id) {
        String sql = "SELECT id, task_id, execution_id, table_name, sync_count, total_count, " +
                     "exception_count, start_time, last_update_time, insert_count, update_count, " +
                     "delete_count, remark, create_time, update_time " +
                     "FROM cdc_table_statistics WHERE id = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSyncStatistics(rs);
                }
            }
        } catch (SQLException e) {
            log.error("查询同步统计数据失败: {}", e.getMessage(), e);
        }
        
        return null;
    }

    @Override
    public List<SyncStatistics> selectByTaskId(Integer taskId) {
        String sql = "SELECT id, task_id, execution_id, table_name, sync_count, total_count, " +
                     "exception_count, start_time, last_update_time, insert_count, update_count, " +
                     "delete_count, remark, create_time, update_time " +
                     "FROM cdc_table_statistics WHERE task_id = ? ORDER BY id ASC";
        
        List<SyncStatistics> result = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, taskId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSetToSyncStatistics(rs));
                }
            }
        } catch (SQLException e) {
            log.error("查询任务同步统计数据失败: {}", e.getMessage(), e);
        }
        
        return result;
    }

    @Override
    public List<SyncStatistics> selectByExecutionId(Integer executionId) {
        String sql = "SELECT id, task_id, execution_id, table_name, sync_count, total_count, " +
                     "exception_count, start_time, last_update_time, insert_count, update_count, " +
                     "delete_count, remark, create_time, update_time " +
                     "FROM cdc_table_statistics WHERE execution_id = ? ORDER BY id ASC";
        
        List<SyncStatistics> result = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, executionId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSetToSyncStatistics(rs));
                }
            }
        } catch (SQLException e) {
            log.error("查询执行记录同步统计数据失败: {}", e.getMessage(), e);
        }
        
        return result;
    }

    @Override
    public SyncStatistics selectByTaskAndTable(Integer taskId, Integer executionId, String tableName) {
        String sql = "SELECT id, task_id, execution_id, table_name, sync_count, total_count, " +
                     "exception_count, start_time, last_update_time, insert_count, update_count, " +
                     "delete_count, remark, create_time, update_time " +
                     "FROM cdc_table_statistics WHERE task_id = ? AND execution_id = ? AND table_name = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, taskId);
            stmt.setInt(2, executionId);
            stmt.setString(3, tableName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSyncStatistics(rs);
                }
            }
        } catch (SQLException e) {
            log.error("查询特定表同步统计数据失败: {}", e.getMessage(), e);
        }
        
        return null;
    }

    @Override
    public SyncStatistics selectByTaskIdAndTableName(Integer taskId, String tableName) {
        String sql = "SELECT id, task_id, execution_id, table_name, sync_count, total_count, " +
                     "exception_count, start_time, last_update_time, insert_count, update_count, " +
                     "delete_count, remark, create_time, update_time " +
                     "FROM cdc_table_statistics WHERE task_id = ? AND table_name = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, taskId);
            stmt.setString(2, tableName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSyncStatistics(rs);
                }
            }
        } catch (SQLException e) {
            log.error("查询任务表同步统计数据失败: {}", e.getMessage(), e);
        }
        
        return null;
    }

    @Override
    public int updateProgress(Integer id, Integer syncCount, Double progress) {
        String sql = "UPDATE cdc_table_statistics SET sync_count = ?, progress = ?, last_update_time = ? WHERE id = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, syncCount);
            stmt.setDouble(2, progress);
            stmt.setTimestamp(3, new Timestamp(new Date().getTime()));
            stmt.setInt(4, id);
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("更新同步进度失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public int updateExceptionCount(Integer id, Integer exceptionCount) {
        String sql = "UPDATE cdc_table_statistics SET exception_count = ?, last_update_time = ? WHERE id = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, exceptionCount);
            stmt.setTimestamp(2, new Timestamp(new Date().getTime()));
            stmt.setInt(3, id);
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("更新异常计数失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public int updateSyncCount(Integer id, Integer syncCount, Integer exceptionCount) {
        String sql = "UPDATE cdc_table_statistics SET sync_count = ?, exception_count = ?, last_update_time = ? WHERE id = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, syncCount);
            stmt.setInt(2, exceptionCount);
            stmt.setTimestamp(3, new Timestamp(new Date().getTime()));
            stmt.setInt(4, id);
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("更新同步计数失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 将ResultSet映射为SyncStatistics对象
     */
    private SyncStatistics mapResultSetToSyncStatistics(ResultSet rs) throws SQLException {
        SyncStatistics stats = new SyncStatistics();
        stats.setId(rs.getInt("id"));
        stats.setTaskId(rs.getInt("task_id"));
        
        Object executionId = rs.getObject("execution_id");
        if (executionId != null) {
            stats.setExecutionId((Integer) executionId);
        }
        
        stats.setTableName(rs.getString("table_name"));
        stats.setSyncCount(rs.getInt("sync_count"));
        stats.setTotalCount(rs.getInt("total_count"));
        stats.setExceptionCount(rs.getInt("exception_count"));
        
        Timestamp startTime = rs.getTimestamp("start_time");
        if (startTime != null) {
            stats.setStartTime(new Date(startTime.getTime()));
        }
        
        Timestamp lastUpdateTime = rs.getTimestamp("last_update_time");
        if (lastUpdateTime != null) {
            stats.setLastUpdateTime(new Date(lastUpdateTime.getTime()));
        }
        
        // 获取新增字段
        stats.setInsertCount(rs.getInt("insert_count"));
        stats.setUpdateCount(rs.getInt("update_count"));
        stats.setDeleteCount(rs.getInt("delete_count"));
        stats.setRemark(rs.getString("remark"));
        
        Timestamp createTime = rs.getTimestamp("create_time");
        if (createTime != null) {
            stats.setCreateTime(new Date(createTime.getTime()));
        }
        
        Timestamp updateTime = rs.getTimestamp("update_time");
        if (updateTime != null) {
            stats.setUpdateTime(new Date(updateTime.getTime()));
        }
        
        return stats;
    }
} 