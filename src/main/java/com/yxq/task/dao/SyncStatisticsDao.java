package com.yxq.task.dao;

import com.yxq.task.entity.SyncStatistics;
import java.util.List;

/**
 * 同步统计DAO接口
 */
public interface SyncStatisticsDao {
    
    /**
     * 插入同步统计信息
     *
     * @param syncStatistics 统计信息
     * @return 影响行数
     */
    int insert(SyncStatistics syncStatistics);
    
    /**
     * 更新同步统计信息
     *
     * @param syncStatistics 统计信息
     * @return 影响行数
     */
    int update(SyncStatistics syncStatistics);
    
    /**
     * 根据ID查询同步统计信息
     *
     * @param id 统计ID
     * @return 统计信息
     */
    SyncStatistics selectById(Integer id);
    
    /**
     * 根据任务ID查询同步统计信息列表
     *
     * @param taskId 任务ID
     * @return 统计信息列表
     */
    List<SyncStatistics> selectByTaskId(Integer taskId);
    
    /**
     * 根据执行记录ID查询同步统计信息
     *
     * @param executionId 执行记录ID
     * @return 统计信息列表
     */
    List<SyncStatistics> selectByExecutionId(Integer executionId);
    
    /**
     * 根据任务ID、执行记录ID和表名查询同步统计信息
     *
     * @param taskId 任务ID
     * @param executionId 执行记录ID
     * @param tableName 表名
     * @return 统计信息
     */
    SyncStatistics selectByTaskAndTable(Integer taskId, Integer executionId, String tableName);
    
    /**
     * 根据任务ID和表名查询同步统计信息
     *
     * @param taskId 任务ID
     * @param tableName 表名
     * @return 统计信息
     */
    SyncStatistics selectByTaskIdAndTableName(Integer taskId, String tableName);
    
    /**
     * 根据ID删除同步统计信息
     *
     * @param id 统计ID
     * @return 影响行数
     */
    int deleteById(Integer id);
    
    /**
     * 根据任务ID删除同步统计信息
     *
     * @param taskId 任务ID
     * @return 影响行数
     */
    int deleteByTaskId(Integer taskId);
    
    /**
     * 更新同步进度
     *
     * @param id 统计ID
     * @param syncCount 同步记录数
     * @param progress 进度百分比
     * @return 影响行数
     */
    int updateProgress(Integer id, Integer syncCount, Double progress);
    
    /**
     * 更新异常记录数
     *
     * @param id 统计ID
     * @param exceptionCount 异常记录数
     * @return 影响行数
     */
    int updateExceptionCount(Integer id, Integer exceptionCount);
    
    /**
     * 更新同步数量和异常数
     *
     * @param id 统计ID
     * @param syncCount 同步记录数
     * @param exceptionCount 异常记录数
     * @return 影响行数
     */
    int updateSyncCount(Integer id, Integer syncCount, Integer exceptionCount);
} 