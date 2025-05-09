package com.yxq.task.service.impl;

import com.yxq.task.dao.SyncStatisticsDao;
import com.yxq.task.entity.SyncStatistics;
import com.yxq.task.service.SyncStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 同步统计服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncStatisticsServiceImpl implements SyncStatisticsService {

    private SyncStatisticsDao syncStatisticsDao;
    
    /**
     * 构造方法
     */
    public SyncStatisticsServiceImpl(SyncStatisticsDao syncStatisticsDao) {
        this.syncStatisticsDao = syncStatisticsDao;
    }

    @Override
    public boolean addStatistics(SyncStatistics syncStatistics) {
        // 设置开始时间和最后更新时间
        Date now = new Date();
        if (syncStatistics.getStartTime() == null) {
            syncStatistics.setStartTime(now);
        }
        syncStatistics.setLastUpdateTime(now);
        
        // 如果没有设置值，默认为0
        if (syncStatistics.getSyncCount() == null) {
            syncStatistics.setSyncCount(0);
        }
        if (syncStatistics.getExceptionCount() == null) {
            syncStatistics.setExceptionCount(0);
        }
        if (syncStatistics.getProgress() == null) {
            syncStatistics.setProgress(0.0);
        }
        
        return syncStatisticsDao.insert(syncStatistics) > 0;
    }

    @Override
    public boolean updateStatistics(SyncStatistics syncStatistics) {
        // 更新最后更新时间
        syncStatistics.setLastUpdateTime(new Date());
        
        return syncStatisticsDao.update(syncStatistics) > 0;
    }

    @Override
    public SyncStatistics getStatisticsById(Integer id) {
        return syncStatisticsDao.selectById(id);
    }

    @Override
    public List<SyncStatistics> getStatisticsByTaskId(Integer taskId) {
        return syncStatisticsDao.selectByTaskId(taskId);
    }

    @Override
    public SyncStatistics getStatisticsByTaskIdAndTableName(Integer taskId, String tableName) {
        return syncStatisticsDao.selectByTaskIdAndTableName(taskId, tableName);
    }

    @Override
    public boolean deleteStatistics(Integer id) {
        return syncStatisticsDao.deleteById(id) > 0;
    }

    @Override
    public boolean deleteStatisticsByTaskId(Integer taskId) {
        return syncStatisticsDao.deleteByTaskId(taskId) > 0;
    }

    @Override
    public boolean updateProgress(Integer taskId, String tableName, Integer syncCount, Double progress) {
        // 查询统计记录
        SyncStatistics statistics = syncStatisticsDao.selectByTaskIdAndTableName(taskId, tableName);
        if (statistics == null) {
            // 如果记录不存在，创建新记录
            statistics = new SyncStatistics();
            statistics.setTaskId(taskId);
            statistics.setTableName(tableName);
            statistics.setSyncCount(syncCount);
            statistics.setProgress(progress);
            statistics.setExceptionCount(0);
            return addStatistics(statistics);
        } else {
            // 如果记录存在，更新进度
            return syncStatisticsDao.updateProgress(statistics.getId(), syncCount, progress) > 0;
        }
    }

    @Override
    public boolean updateExceptionCount(Integer taskId, String tableName, Integer exceptionCount) {
        // 查询统计记录
        SyncStatistics statistics = syncStatisticsDao.selectByTaskIdAndTableName(taskId, tableName);
        if (statistics == null) {
            // 如果记录不存在，创建新记录
            statistics = new SyncStatistics();
            statistics.setTaskId(taskId);
            statistics.setTableName(tableName);
            statistics.setSyncCount(0);
            statistics.setProgress(0.0);
            statistics.setExceptionCount(exceptionCount);
            return addStatistics(statistics);
        } else {
            // 如果记录存在，更新异常数
            return syncStatisticsDao.updateExceptionCount(statistics.getId(), exceptionCount) > 0;
        }
    }
} 