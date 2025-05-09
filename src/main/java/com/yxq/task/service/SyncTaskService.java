package com.yxq.task.service;

import com.yxq.task.entity.SyncTask;
import java.util.List;
import java.util.Map;

/**
 * 同步任务服务接口
 */
public interface SyncTaskService {
    
    /**
     * 添加同步任务
     *
     * @param syncTask 同步任务信息
     * @return 添加是否成功
     */
    boolean addSyncTask(SyncTask syncTask);
    
    /**
     * 更新同步任务
     *
     * @param syncTask 同步任务信息
     * @return 更新是否成功
     */
    boolean updateSyncTask(SyncTask syncTask);
    
    /**
     * 删除同步任务
     *
     * @param id 任务ID
     * @return 删除是否成功
     */
    boolean deleteSyncTask(Integer id);
    
    /**
     * 根据ID获取同步任务
     *
     * @param id 任务ID
     * @return 同步任务信息
     */
    SyncTask getSyncTaskById(Integer id);
    
    /**
     * 获取所有同步任务列表
     *
     * @return 同步任务列表
     */
    List<SyncTask> getAllSyncTasks();
    
    /**
     * 修改任务状态
     *
     * @param id 任务ID
     * @param status 状态（0=停止 1=运行中 2=异常）
     * @return 修改是否成功
     */
    boolean updateStatus(Integer id, Integer status);
    
    /**
     * 启动同步任务
     *
     * @param id 任务ID
     * @return 启动是否成功
     */
    boolean startTask(Integer id);
    
    /**
     * 停止同步任务
     *
     * @param id 任务ID
     * @return 停止是否成功
     */
    boolean stopTask(Integer id);
    
    /**
     * 获取任务监控统计信息
     *
     * @param id 任务ID
     * @return 监控统计信息
     */
    Map<String, Object> getTaskStatistics(Integer id);
} 