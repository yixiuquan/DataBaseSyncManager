package com.yxq.task.service;

import com.yxq.task.entity.SyncException;
import java.util.List;

/**
 * 同步异常服务接口
 */
public interface SyncExceptionService {
    
    /**
     * 添加同步异常记录
     *
     * @param exception 异常信息
     * @return 是否成功
     */
    boolean addException(SyncException exception);
    
    /**
     * 根据任务ID获取异常记录列表
     *
     * @param taskId 任务ID
     * @return 异常记录列表
     */
    List<SyncException> getByTaskId(Integer taskId);
    
    /**
     * 根据任务ID和表名获取异常记录列表
     *
     * @param taskId 任务ID
     * @param tableName 表名
     * @return 异常记录列表
     */
    List<SyncException> getByTaskIdAndTableName(Integer taskId, String tableName);
    
    /**
     * 统计任务的异常数量
     *
     * @param taskId 任务ID
     * @return 异常数量
     */
    int countByTaskId(Integer taskId);
    
    /**
     * 统计任务指定表的异常数量
     *
     * @param taskId 任务ID
     * @param tableName 表名
     * @return 异常数量
     */
    int countByTaskIdAndTableName(Integer taskId, String tableName);
} 