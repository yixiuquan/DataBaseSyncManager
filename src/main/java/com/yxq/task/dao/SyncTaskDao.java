package com.yxq.task.dao;

import com.yxq.task.entity.SyncTask;
import java.sql.Timestamp;
import java.util.List;

/**
 * 同步任务DAO接口
 */
public interface SyncTaskDao {
    
    /**
     * 插入同步任务
     *
     * @param syncTask 同步任务信息
     * @return 影响行数
     */
    int insert(SyncTask syncTask);
    
    /**
     * 更新同步任务
     *
     * @param syncTask 同步任务信息
     * @return 影响行数
     */
    int update(SyncTask syncTask);
    
    /**
     * 根据ID删除同步任务
     *
     * @param id 任务ID
     * @return 影响行数
     */
    int deleteById(Integer id);
    
    /**
     * 根据ID查询同步任务
     *
     * @param id 任务ID
     * @return 同步任务
     */
    SyncTask selectById(Integer id);
    
    /**
     * 查询所有同步任务
     *
     * @return 同步任务列表
     */
    List<SyncTask> selectAll();
    
    /**
     * 更新任务状态
     *
     * @param id 任务ID
     * @param status 状态（0-停止，1-运行中，2-异常）
     * @return 影响行数
     */
    int updateStatus(Integer id, Integer status);
    
    /**
     * 更新任务开始时间
     *
     * @param id 任务ID
     * @param startTime 开始时间
     * @return 影响行数
     */
    int updateTaskStartTime(Integer id, Timestamp startTime);
} 