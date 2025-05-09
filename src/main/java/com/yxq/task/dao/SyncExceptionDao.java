package com.yxq.task.dao;

import com.yxq.task.entity.SyncException;
import java.util.List;

/**
 * 同步异常DAO接口
 */
public interface SyncExceptionDao {
    
    /**
     * 插入同步异常记录
     *
     * @param exception 异常信息
     * @return 影响行数
     */
    int insert(SyncException exception);
    
    /**
     * 更新异常记录
     *
     * @param syncException 异常记录
     * @return 影响行数
     */
    int update(SyncException syncException);
    
    /**
     * 根据ID删除异常记录
     *
     * @param id 记录ID
     * @return 影响行数
     */
    int deleteById(Integer id);
    
    /**
     * 根据任务ID删除异常记录
     *
     * @param taskId 任务ID
     * @return 影响行数
     */
    int deleteByTaskId(Integer taskId);
    
    /**
     * 根据ID查询异常记录
     *
     * @param id 记录ID
     * @return 异常记录
     */
    SyncException selectById(Integer id);
    
    /**
     * 根据任务ID查询异常记录列表
     *
     * @param taskId 任务ID
     * @return 异常记录列表
     */
    List<SyncException> selectByTaskId(Integer taskId);
    
    /**
     * 根据执行记录ID查询异常记录列表
     *
     * @param executionId 执行记录ID
     * @return 异常记录列表
     */
    List<SyncException> selectByExecutionId(Integer executionId);
    
    /**
     * 根据任务ID和表名查询异常记录列表
     *
     * @param taskId 任务ID
     * @param tableName 表名
     * @return 异常记录列表
     */
    List<SyncException> selectByTaskIdAndTableName(Integer taskId, String tableName);
    
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