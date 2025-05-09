package com.yxq.task.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.yxq.task.dao.SyncExceptionDao;
import com.yxq.task.dao.SyncTaskDao;
import com.yxq.task.dao.SyncStatisticsDao;
import com.yxq.task.entity.Database;
import com.yxq.task.entity.SyncException;
import com.yxq.task.entity.SyncStatistics;
import com.yxq.task.entity.SyncTask;
import com.yxq.task.flink.FlinkCDCMain;
import com.yxq.task.flink.FlinkCDCSink;
import com.yxq.task.service.DatabaseService;
import com.yxq.task.service.SyncTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 同步任务服务实现类
 */
@Slf4j
@Service
public class SyncTaskServiceImpl implements SyncTaskService {

    @Autowired
    private FlinkCDCMain flinkCDCMain;

    private final SyncTaskDao syncTaskDao;
    private final SyncExceptionDao syncExceptionDao;
    private final DatabaseService databaseService;
    private final SyncStatisticsDao syncStatisticsDao;
    
    // 存储运行中的任务
    private static final Map<Integer, Object> RUNNING_TASKS = new HashMap<>();
    
    /**
     * 构造方法
     */
    public SyncTaskServiceImpl(SyncTaskDao syncTaskDao, SyncExceptionDao syncExceptionDao, 
                              DatabaseService databaseService, SyncStatisticsDao syncStatisticsDao) {
        this.syncTaskDao = syncTaskDao;
        this.syncExceptionDao = syncExceptionDao;
        this.databaseService = databaseService;
        this.syncStatisticsDao = syncStatisticsDao;
    }

    @Override
    public boolean addSyncTask(SyncTask syncTask) {
        // 设置创建时间
        Date now = new Date();
        syncTask.setCreateTime(now);
        // 默认停止状态
        syncTask.setStatus(0);
        
        return syncTaskDao.insert(syncTask) > 0;
    }

    @Override
    public boolean updateSyncTask(SyncTask syncTask) {
        // 获取原始任务
        SyncTask oldTask = syncTaskDao.selectById(syncTask.getId());
        if (oldTask == null) {
            return false;
        }
        
        // 如果任务正在运行，不允许修改
        if (oldTask.getStatus() == 1) {
            return false;
        }
        
        return syncTaskDao.update(syncTask) > 0;
    }

    @Override
    public boolean deleteSyncTask(Integer id) {
        // 获取原始任务
        SyncTask task = syncTaskDao.selectById(id);
        if (task == null) {
            return false;
        }
        
        // 如果任务正在运行，不允许删除
        if (task.getStatus() == 1) {
            return false;
        }
        
        // 删除任务相关的异常记录
        syncExceptionDao.deleteByTaskId(id);
        
        return syncTaskDao.deleteById(id) > 0;
    }

    @Override
    public SyncTask getSyncTaskById(Integer id) {
        return syncTaskDao.selectById(id);
    }

    @Override
    public List<SyncTask> getAllSyncTasks() {
        return syncTaskDao.selectAll();
    }

    @Override
    public boolean updateStatus(Integer id, Integer status) {
        return syncTaskDao.updateStatus(id, status) > 0;
    }

    @Override
    public boolean startTask(Integer id) {
        // 获取任务信息
        SyncTask task = syncTaskDao.selectById(id);
        if (task == null) {
            return false;
        }
        
        // 如果任务已经在运行中，直接返回成功
        if (task.getStatus() == 1) {
            return true;
        }
        
        // 检查源数据库和目标数据库是否可用
        Database sourceDb = databaseService.getDatabaseById(task.getSourceDbId());
        Database targetDb = databaseService.getDatabaseById(task.getTargetDbId());
        
        if (sourceDb == null || targetDb == null) {
            log.error("任务[{}]启动失败：数据库配置不存在", task.getTaskName());
            return false;
        }
        
        if (sourceDb.getStatus() == 0 || targetDb.getStatus() == 0) {
            log.error("任务[{}]启动失败：数据库已停用", task.getTaskName());
            return false;
        }
        
        try {
            // 使用FlinkCDCMain启动同步任务
            boolean started = flinkCDCMain.startSyncTask(task, sourceDb, targetDb);
            
            if (started) {
                // 更新任务状态为运行中
                boolean b = updateStatus(id, 1);
                if (b){
                    log.info("更新任务状态为运行中成功");
                } else {
                    log.error("更新任务状态为运行中失败");
                }
                // 记录任务到运行列表
                RUNNING_TASKS.put(id, new Object());
                
                log.info("任务[{}]已成功启动", task.getTaskName());
                return true;
            } else {
                log.error("任务[{}]启动失败", task.getTaskName());
                // 更新任务状态为异常
                task.setStatus(2);
                syncTaskDao.update(task);
                
                // 记录异常信息
                SyncException exception = new SyncException();
                exception.setTaskId(id);
                exception.setErrorMessage("任务启动失败");
                exception.setErrorTime(new Date());
                syncExceptionDao.insert(exception);
                
                return false;
            }
        } catch (Exception e) {
            log.error("任务[{}]启动异常: {}", task.getTaskName(), e.getMessage(), e);
            // 更新任务状态为异常
            task.setStatus(2);
            syncTaskDao.update(task);
            
            // 记录异常信息
            SyncException exception = new SyncException();
            exception.setTaskId(id);
            exception.setErrorMessage("任务启动异常: " + e.getMessage());
            exception.setErrorTime(new Date());
            syncExceptionDao.insert(exception);
            
            return false;
        }
    }

    @Override
    public boolean stopTask(Integer id) {
        // 获取任务信息
        SyncTask task = syncTaskDao.selectById(id);
        if (task == null) {
            return false;
        }
        
        // 如果任务不在运行中，直接返回成功
        if (task.getStatus() != 1) {
            return true;
        }
        
        try {
            // 使用FlinkCDCMain停止同步任务
            boolean stopped = FlinkCDCMain.stopSyncTask(id);
            
            if (stopped) {
                // 从运行列表中移除任务
                RUNNING_TASKS.remove(id);
                
                // 更新任务状态为已停止
                task.setStatus(0);
                syncTaskDao.update(task);
                
                log.info("任务[{}]已成功停止", task.getTaskName());
                return true;
            } else {
                log.error("任务[{}]停止失败", task.getTaskName());
                return false;
            }
        } catch (Exception e) {
            log.error("任务[{}]停止异常: {}", task.getTaskName(), e.getMessage(), e);
            // 更新任务状态为异常
            task.setStatus(2);
            syncTaskDao.update(task);
            
            // 记录异常信息
            SyncException exception = new SyncException();
            exception.setTaskId(id);
            exception.setErrorMessage("任务停止异常: " + e.getMessage());
            exception.setErrorTime(new Date());
            syncExceptionDao.insert(exception);
            
            return false;
        }
    }

    @Override
    public Map<String, Object> getTaskStatistics(Integer id) {
        // 获取任务信息
        SyncTask task = syncTaskDao.selectById(id);
        if (task == null) {
            return null;
        }
        
        // 检查任务是否实际在运行，并更新状态
        boolean isTaskRunning = FlinkCDCMain.isTaskRunning(id);
        log.info("获取任务[{}]统计信息, 数据库状态:{}, Flink运行状态:{}", task.getTaskName(), task.getStatus(), isTaskRunning ? "运行中" : "已停止");
        
        // 如果Flink报告任务正在运行，但数据库状态不是运行中，则更新数据库状态
        if (isTaskRunning && task.getStatus() != 1) {
            log.info("任务[{}]实际正在运行，但数据库状态为{}，更新为运行中状态", 
                    task.getTaskName(), task.getStatus());
            task.setStatus(1);
            syncTaskDao.updateStatus(id, 1);
            // 重新获取任务以确保状态最新
            task = syncTaskDao.selectById(id);
        } 
        // 如果Flink报告任务已停止，但数据库状态是运行中，则更新数据库状态
        else if (!isTaskRunning && task.getStatus() == 1) {
            log.info("任务[{}]实际已停止，但数据库状态为运行中，更新为停止状态", task.getTaskName());
            task.setStatus(0);
            syncTaskDao.updateStatus(id, 0);
            // 重新获取任务以确保状态最新
            task = syncTaskDao.selectById(id);
        }
        
        Map<String, Object> result = new HashMap<>();
        
        // 获取真实统计数据
        List<Map<String, Object>> tableStats = new ArrayList<>();
        int totalSyncCount = 0; // 总同步数据量
        
        try {
            // 获取Flink CDC Sink实例
            FlinkCDCSink cdcSink = null;
            if (isTaskRunning) {
                // 增强日志：打印当前所有FlinkCDCSink实例
                try {
                    // 使用反射获取INSTANCES字段
                    java.lang.reflect.Field instancesField = FlinkCDCSink.class.getDeclaredField("INSTANCES");
                    instancesField.setAccessible(true);
                    Object instancesObj = instancesField.get(null);
                    log.info("当前所有FlinkCDCSink实例: {}", instancesObj);
                } catch (Exception e) {
                    log.warn("获取实例映射异常: {}", e.getMessage());
                }
                
                cdcSink = FlinkCDCSink.getInstance(id);
//                log.info("获取任务[{}]的CDC Sink实例: {}", task.getTaskName(), cdcSink != null ? "成功" : "失败");
                
                if (cdcSink == null) {
                    log.warn("任务[{}]实例获取失败，尝试手动恢复实例...", task.getTaskName());
                    // 尝试手动恢复实例（这只是临时解决方案，实际应优化FlinkCDCMain中的实例管理）
                    // 这里不会立即生效，但下次调用时可能会有效
                    try {
                        // 使用反射获取和修复实例
                        java.lang.reflect.Method method = FlinkCDCMain.class.getDeclaredMethod("repairCDCSinkInstance", Integer.class);
                        method.setAccessible(true);
                        method.invoke(null, id);
                    } catch (Exception e) {
                        log.warn("尝试修复CDC Sink实例失败: {}", e.getMessage());
                    }
                }
            }
            
            // 解析任务配置的表，使用TypeReference指定泛型类型
            List<Map<String, String>> tables = new ArrayList<>();
            String tablesStr = task.getTables();
            
            // 检查是否是JSON格式
            if (tablesStr != null && !tablesStr.isEmpty()) {
                if (tablesStr.trim().startsWith("[")) {
                    // JSON数组格式
                    try {
                        tables = JSON.parseObject(tablesStr, 
                            new TypeReference<List<Map<String, String>>>() {});
                    } catch (Exception e) {
                        log.error("解析表配置JSON异常: {}", e.getMessage());
                        // 使用逗号分隔的字符串格式作为备选
                        String[] tableNames = tablesStr.split(",");
                        for (String tableName : tableNames) {
                            if (!tableName.trim().isEmpty()) {
                                Map<String, String> config = new HashMap<>();
                                config.put("sourceTable", tableName.trim());
                                config.put("targetTable", tableName.trim()); // 默认使用相同表名
                                tables.add(config);
                            }
                        }
                    }
                } else {
                    // 逗号分隔的字符串格式
                    String[] tableNames = tablesStr.split(",");
                    for (String tableName : tableNames) {
                        if (!tableName.trim().isEmpty()) {
                            Map<String, String> config = new HashMap<>();
                            config.put("sourceTable", tableName.trim());
                            config.put("targetTable", tableName.trim()); // 默认使用相同表名
                            tables.add(config);
                        }
                    }
                }
            }
            
            if (tables != null) {
                for (Map<String, String> table : tables) {
                    String sourceTable = table.get("sourceTable");
                    String targetTable = table.get("targetTable");
                    
                    Map<String, Object> tableStat = new HashMap<>();
                    tableStat.put("tableName", sourceTable);
                    tableStat.put("startTime", task.getTaskStartTime());
                    
                    // 获取同步统计数据 - 优先从数据库获取持久化的统计数据
                    int insertCount = 0;
                    int updateCount = 0;
                    int deleteCount = 0;
                    int exceptionCount = 0;
                    int syncCount = 0;
                    Date lastUpdateTime = null;
                    
                    // 首先尝试从数据库获取持久化的统计数据
                    try {
                        SyncStatistics stats = syncStatisticsDao.selectByTaskIdAndTableName(id, sourceTable);
                        if (stats != null) {
                            // 使用数据库中的统计数据
                            insertCount = stats.getInsertCount() != null ? stats.getInsertCount() : 0;
                            updateCount = stats.getUpdateCount() != null ? stats.getUpdateCount() : 0;
                            deleteCount = stats.getDeleteCount() != null ? stats.getDeleteCount() : 0;
                            exceptionCount = stats.getExceptionCount() != null ? stats.getExceptionCount() : 0;
                            syncCount = stats.getSyncCount() != null ? stats.getSyncCount() : 0;
                            lastUpdateTime = stats.getLastUpdateTime();
                            
                            log.debug("从数据库获取任务[{}]表[{}]统计数据: 插入={}, 更新={}, 删除={}", 
                                   task.getTaskName(), sourceTable, insertCount, updateCount, deleteCount);
                        }
                    } catch (Exception e) {
                        log.warn("从数据库获取统计数据异常，尝试从CDC Sink获取: {}", e.getMessage());
                    }
                    
                    // 如果数据库中没有数据或数据可能已过时，并且CDC Sink实例可用，则尝试从实例获取最新数据
                    if (cdcSink != null) {
                        // 获取CDC Sink中的最新统计数据
                        int sinkInsertCount = cdcSink.getInsertCount(sourceTable);
                        int sinkUpdateCount = cdcSink.getUpdateCount(sourceTable);
                        int sinkDeleteCount = cdcSink.getDeleteCount(sourceTable);
                        int sinkErrorCount = cdcSink.getErrorCount(sourceTable);
                        
                        // 如果CDC Sink中的数据更新，则使用CDC Sink的数据
                        if (sinkInsertCount > insertCount || sinkUpdateCount > updateCount || 
                            sinkDeleteCount > deleteCount || sinkErrorCount > exceptionCount) {
                            insertCount = sinkInsertCount;
                            updateCount = sinkUpdateCount;
                            deleteCount = sinkDeleteCount;
                            exceptionCount = sinkErrorCount;
                            syncCount = insertCount + updateCount + deleteCount;
                            lastUpdateTime = new Date(); // 使用当前时间
                            
                            log.debug("从CDC Sink获取任务[{}]表[{}]实时统计数据: 插入={}, 更新={}, 删除={}", 
                                   task.getTaskName(), sourceTable, insertCount, updateCount, deleteCount);
                        }
                    }
                    
                    // 计算总同步数
                    totalSyncCount += syncCount;
                    
                    // 计算同步进度
                    double progress = calculateProgress(task, sourceTable, syncCount);
                    
                    // 设置统计数据
                    tableStat.put("insertCount", insertCount);
                    tableStat.put("updateCount", updateCount);
                    tableStat.put("deleteCount", deleteCount);
                    tableStat.put("syncCount", syncCount);
                    tableStat.put("exceptionCount", exceptionCount);
                    tableStat.put("progress", progress);
                    tableStat.put("lastUpdateTime", lastUpdateTime != null ? lastUpdateTime : new Date());
                    
                    tableStats.add(tableStat);
                }
            }
            
            // 设置任务信息
            Map<String, Object> taskInfoMap = new HashMap<>();
            taskInfoMap.put("totalSyncCount", totalSyncCount);
            taskInfoMap.put("taskName", task.getTaskName());
            taskInfoMap.put("status", task.getStatus());
            taskInfoMap.put("isRunning", isTaskRunning);
            taskInfoMap.put("syncType", task.getSyncType());
            taskInfoMap.put("taskStartTime", task.getTaskStartTime());
            
            result.put("taskInfo", taskInfoMap);
            result.put("tableStats", tableStats);
            
            // 计算总异常数
            int totalExceptionCount = syncExceptionDao.countByTaskId(id);
            result.put("totalExceptionCount", totalExceptionCount);
            
            return result;
        } catch (Exception e) {
            log.error("获取任务统计信息异常: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 计算同步进度
     */
    private double calculateProgress(SyncTask task, String sourceTable, int syncCount) {
        // 增量同步任务通常无法准确计算进度，返回-1表示持续进行中
        if (task.getSyncType() == 1) {
            return -1;
        }
        
        // 全量同步任务，使用简化的进度计算
        // 这里假设每个表有一个合理的数据量范围，以便计算进度
        // 实际项目中应该根据真实情况计算
        try {
            // 模拟每个表的总记录数在1万-5万之间
            int estimatedTotal = 10000 + (sourceTable.hashCode() % 40000);
            if (estimatedTotal <= 0) estimatedTotal = 20000; // 确保为正数
            
            // 计算进度百分比
            return Math.min(100.0, (syncCount * 100.0) / estimatedTotal);
        } catch (Exception e) {
            log.error("计算同步进度异常: {}", e.getMessage());
        }
        
        // 默认返回适中的进度值
        return 50.0;
    }
} 