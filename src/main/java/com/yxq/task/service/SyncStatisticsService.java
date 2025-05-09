package com.yxq.task.service;

import com.yxq.task.entity.SyncStatistics;
import java.util.List;

/**
 * 同步统计服务接口
 */
public interface SyncStatisticsService {
    
    /**
     * 添加同步统计信息
     *
     * @param syncStatistics 统计信息
     * @return 添加是否成功
     */
    boolean addStatistics(SyncStatistics syncStatistics);
    
    /**
     * 更新同步统计信息
     *
     * @param syncStatistics 统计信息
     * @return 更新是否成功
     */
    boolean updateStatistics(SyncStatistics syncStatistics);
    
    /**
     * 根据ID获取同步统计信息
     *
     * @param id 统计ID
     * @return 统计信息
     */
    SyncStatistics getStatisticsById(Integer id);
    
    /**
     * 根据任务ID获取同步统计信息列表
     *
     * @param taskId 任务ID
     * @return 统计信息列表
     */
    List<SyncStatistics> getStatisticsByTaskId(Integer taskId);
    
    /**
     * 根据任务ID和表名获取同步统计信息
     *
     * @param taskId 任务ID
     * @param tableName 表名
     * @return 统计信息
     */
    SyncStatistics getStatisticsByTaskIdAndTableName(Integer taskId, String tableName);
    
    /**
     * 删除同步统计信息
     *
     * @param id 统计ID
     * @return 删除是否成功
     */
    boolean deleteStatistics(Integer id);
    
    /**
     * 根据任务ID删除同步统计信息
     *
     * @param taskId 任务ID
     * @return 删除是否成功
     */
    boolean deleteStatisticsByTaskId(Integer taskId);
    
    /**
     * 更新同步进度
     *
     * @param taskId 任务ID
     * @param tableName 表名
     * @param syncCount 同步记录数
     * @param progress 进度百分比
     * @return 更新是否成功
     */
    boolean updateProgress(Integer taskId, String tableName, Integer syncCount, Double progress);
    
    /**
     * 更新异常记录数
     *
     * @param taskId 任务ID
     * @param tableName 表名
     * @param exceptionCount 异常记录数
     * @return 更新是否成功
     */
    boolean updateExceptionCount(Integer taskId, String tableName, Integer exceptionCount);
} 