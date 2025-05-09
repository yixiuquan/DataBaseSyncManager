package com.yxq.task.service.impl;

import com.yxq.task.dao.SyncExceptionDao;
import com.yxq.task.entity.SyncException;
import com.yxq.task.service.SyncExceptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 同步异常服务实现类
 */
@Slf4j
@Service
public class SyncExceptionServiceImpl implements SyncExceptionService {

    @Autowired
    private SyncExceptionDao syncExceptionDao;
    
    @Override
    public boolean addException(SyncException exception) {
        try {
            return syncExceptionDao.insert(exception) > 0;
        } catch (Exception e) {
            log.error("添加异常记录失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<SyncException> getByTaskId(Integer taskId) {
        try {
            return syncExceptionDao.selectByTaskId(taskId);
        } catch (Exception e) {
            log.error("获取任务异常记录失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<SyncException> getByTaskIdAndTableName(Integer taskId, String tableName) {
        try {
            return syncExceptionDao.selectByTaskIdAndTableName(taskId, tableName);
        } catch (Exception e) {
            log.error("获取任务表异常记录失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public int countByTaskId(Integer taskId) {
        try {
            return syncExceptionDao.countByTaskId(taskId);
        } catch (Exception e) {
            log.error("统计任务异常数量失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public int countByTaskIdAndTableName(Integer taskId, String tableName) {
        try {
            return syncExceptionDao.countByTaskIdAndTableName(taskId, tableName);
        } catch (Exception e) {
            log.error("统计任务表异常数量失败: {}", e.getMessage(), e);
            return 0;
        }
    }
} 