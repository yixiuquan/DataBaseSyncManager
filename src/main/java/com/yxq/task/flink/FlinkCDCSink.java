package com.yxq.task.flink;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yxq.task.dao.DatabaseDao;
import com.yxq.task.dao.SyncExceptionDao;
import com.yxq.task.dao.SyncStatisticsDao;
import com.yxq.task.entity.Database;
import com.yxq.task.entity.SyncException;
import com.yxq.task.entity.SyncStatistics;
import com.yxq.task.util.AESUtil;
import com.yxq.task.util.DbUtil;
import com.yxq.task.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Flink CDC数据同步Sink
 */
@Slf4j
public class FlinkCDCSink extends RichSinkFunction<String> {

    private final Integer taskId;
    private final String targetDb;
    private final String targetHostPort;
    private final String tableMapping;
    private SyncExceptionDao syncExceptionDao;
    private DatabaseDao databaseDao;
    private SyncStatisticsDao syncStatisticsDao;

    // 用于定期保存统计数据的调度器
    private ScheduledExecutorService scheduler;

    // 目标数据库连接信息
    private String targetDbUrl;
    private String targetDbUsername;
    private String targetDbPassword;

    // 使用ConcurrentHashMap存储每个表的计数器
    private final ConcurrentHashMap<String, AtomicInteger> tableInsertCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> tableUpdateCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> tableDeleteCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> tableErrorCount = new ConcurrentHashMap<>();

    // 总体计数器
    private final AtomicInteger insertCount = new AtomicInteger(0);
    private final AtomicInteger updateCount = new AtomicInteger(0);
    private final AtomicInteger deleteCount = new AtomicInteger(0);
    private final AtomicInteger errorCount = new AtomicInteger(0);

    // 静态实例映射，用于获取运行中的sink实例
    private static final ConcurrentHashMap<Integer, FlinkCDCSink> INSTANCES = new ConcurrentHashMap<>();

    // 新增：关闭标志
    private volatile boolean closed = false;

    /**
     * 构造方法
     *
     * @param taskId 任务ID
     * @param targetDb 目标数据库
     * @param tableMapping 表映射关系（JSON格式）
     */
    public FlinkCDCSink(Integer taskId, String targetDb, String targetHostPort, String tableMapping) {
        this.taskId = taskId;
        this.targetDb = targetDb;
        this.targetHostPort = targetHostPort;
        this.tableMapping = tableMapping;

        // 记录实例
        INSTANCES.put(taskId, this);

        // 输出构造参数日志
        log.info("FlinkCDCSink创建 - 任务ID: {}, 目标DB: {}, 表映射: {}", taskId, targetDb, tableMapping);
    }

    // 初始化方法
    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        log.info("启动Flink CDC Sink，任务ID：{}，目标数据库：{}, 表映射：{}", taskId, targetDb, tableMapping);

        // 打印所有实例信息
        log.info("当前运行中的CDC Sink实例: {}", INSTANCES.keySet());

        // 尝试使用DI容器获取SyncExceptionDao实例
        try {
            syncExceptionDao = SpringContextUtil.getBean(SyncExceptionDao.class);
            log.info("成功获取SyncExceptionDao实例");

            // 获取DatabaseDao实例
            databaseDao = SpringContextUtil.getBean(DatabaseDao.class);
            log.info("成功获取DatabaseDao实例");

            // 获取SyncStatisticsDao实例
            syncStatisticsDao = SpringContextUtil.getBean(SyncStatisticsDao.class);
            log.info("成功获取SyncStatisticsDao实例");

            // 获取目标数据库连接信息
            if (databaseDao != null) {
                try {
                    // 根据targetDb查询数据库配置表获取目标数据库信息
                    List<Database> dbList = databaseDao.selectAll();
                    for (Database db : dbList) {
                        if (targetDb.equals(db.getDbName()) && targetHostPort.equals(db.getHost() + ":" + db.getPort())) {
                            // 构建JDBC URL
                            String param = StringUtils.isNotEmpty(db.getParam()) ? db.getParam() : "useUnicode=true&characterEncoding=UTF-8&allowMultiQueries=true&useSSL=false";
                            targetDbUrl = String.format("jdbc:mysql://%s:%d/%s?" + param, db.getHost(), db.getPort(), db.getDbName());
                            targetDbUsername = db.getUsername();
                            targetDbPassword = AESUtil.decrypt(db.getPassword());

                            log.info("目标数据库连接信息已加载: URL={}, 用户名={}", targetDbUrl, targetDbUsername);
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.error("获取目标数据库连接信息失败: {}", e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.warn("无法获取必要的Bean实例，某些功能可能不可用: {}", e.getMessage());
        }

        // 测试数据库连接
        try {
            log.info("测试目标数据库[{}]连接...", targetDb);
            String testSql = "SELECT 1";
            if (targetDbUrl != null) {
                // 使用目标数据库连接测试
                boolean isConnected = false;
                try {
                    java.sql.Connection conn = DbUtil.getConnection(targetDbUrl, targetDbUsername, targetDbPassword);
                    isConnected = conn != null && !conn.isClosed();
                    if (conn != null) {
                        conn.close();
                    }
                } catch (Exception e) {
                    log.error("目标数据库连接测试失败: {}", e.getMessage(), e);
                }
                log.info("目标数据库连接测试结果: {}", isConnected);
            } else {
                // 回退使用默认数据源
                List<Map<String, Object>> result = DbUtil.executeQuery(testSql);
                log.info("默认数据源连接测试结果: {}", !result.isEmpty());
            }
        } catch (Exception e) {
            log.error("测试数据库连接失败: {}", e.getMessage(), e);
        }

        // 启动定期保存统计数据的调度任务
        startStatisticsSavingTask();
    }

    @Override
    public void invoke(String value, Context context) throws Exception {
        // 增强日志，确保记录所有CDC事件
        log.info("收到CDC数据: {}", value);

        try {
            JSONObject obj = JSONObject.parseObject(value);
            String sourceTable = obj.getString("tableName");
            String op = obj.getString("op");
            String sourceDb = obj.getString("db");

            log.info("处理CDC事件: 操作={}, 数据库={}, 表={}, 任务ID={}", op, sourceDb, sourceTable, taskId);

            // 跳过可能导致NPE的无效事件
            if (obj == null || (op != null && "d".equals(op) && obj.getJSONObject("before") == null)) {
                log.warn("跳过无效的删除事件，数据结构不完整: {}", value);
                return;
            }

            // 获取目标表名（通过映射关系）
            String targetTable = getTargetTable(sourceTable);
            if (StringUtils.isEmpty(targetTable)) {
                log.warn("未找到表[{}]的映射关系，跳过处理, 任务ID={}", sourceTable, taskId);
                return;
            }

            log.info("映射表: 源表={}，目标表={}, 任务ID={}", sourceTable, targetTable, taskId);

            // 处理SQL
            String sql = "";
            if ("c".equals(op) || "r".equals(op) || "u".equals(op)) {
                JSONObject afterObj = obj.getJSONObject("after");
                if (afterObj == null || afterObj.isEmpty()) {
                    log.warn("操作[{}]的after数据为空，跳过处理", op);
                    return;
                }

                log.info("变更后数据: {}", afterObj);

                String columns = "";
                String vals = "";
                String updates = "";

                for (Map.Entry<String, Object> entry : afterObj.entrySet()) {
                    String key = entry.getKey();
                    Object valObj = entry.getValue();

                    // 处理日期时间类型
                    if (valObj != null && (key.endsWith("_time") || key.endsWith("_date") || key.equals("create_time") || key.equals("update_time"))) {
                        // 1. 处理字符串类型
                        if (valObj instanceof String) {
                            String val = valObj.toString().trim();
                            if (val.isEmpty() || "null".equalsIgnoreCase(val)) {
                                valObj = null;
                            } else if (val.contains("T")) {
                                // 处理ISO格式
                                val = val.replace("T", " ").replace("Z", "");
                                valObj = val;
                            } else if (val.matches("\\d{4}[-/]\\d{2}[-/]\\d{2}.*")) {
                                // 处理 yyyy-MM-dd HH:mm:ss 或 yyyy/MM/dd HH:mm:ss
                                val = val.replace("/", "-");
                                valObj = val;
                            }
                        }
                        // 2. 处理时间戳类型
                        else if (valObj instanceof Long || valObj instanceof Integer) {
                            long timestamp = Long.parseLong(valObj.toString());
                            Instant instant = Instant.ofEpochMilli(timestamp);
                            ZoneId zoneId = ZoneId.of("Asia/Shanghai");
                            LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, zoneId);
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                            valObj = localDateTime.format(formatter);
                        }
                        // 3. 处理其他类型（如 java.util.Date）
                        else if (valObj instanceof Date) {
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                            LocalDateTime localDateTime = ((Date) valObj).toInstant().atZone(ZoneId.of("Asia/Shanghai")).toLocalDateTime();
                            valObj = localDateTime.format(formatter);
                        }
                        // 4. 回写
                        afterObj.put(key, valObj);
                    }

                    // 构建SQL片段
                    columns += "`" + key + "`,";
                    if (valObj == null) {
                        vals += "NULL,";
                        updates += "`" + key + "`=NULL,";
                    } else if (valObj instanceof String) {
                        // 转义单引号，防止SQL注入
                        String safeVal = valObj.toString().replace("'", "''");
                        vals += "'" + safeVal + "',";
                        updates += "`" + key + "`='" + safeVal + "',";
                    } else {
                        vals += valObj + ",";
                        updates += "`" + key + "`=" + valObj + ",";
                    }
                }

                // 去除最后的逗号
                if (columns.endsWith(",")) {
                    columns = columns.substring(0, columns.length() - 1);
                }
                if (vals.endsWith(",")) {
                    vals = vals.substring(0, vals.length() - 1);
                }
                if (updates.endsWith(",")) {
                    updates = updates.substring(0, updates.length() - 1);
                }

                // 生成INSERT或UPDATE SQL
                sql = "INSERT INTO `" + targetDb + "`.`" + targetTable + "` (" + columns + ") VALUES (" + vals + ")" +
                        " ON DUPLICATE KEY UPDATE " + updates;

                // 更新统计信息
                if ("c".equals(op) || "r".equals(op)) {
                    insertCount.incrementAndGet();
                    // 更新表级别计数器
                    tableInsertCount.computeIfAbsent(sourceTable, k -> new AtomicInteger(0)).incrementAndGet();
                } else {
                    updateCount.incrementAndGet();
                    // 更新表级别计数器
                    tableUpdateCount.computeIfAbsent(sourceTable, k -> new AtomicInteger(0)).incrementAndGet();
                }
            } else if ("d".equals(op)) {
                // 处理删除操作
                JSONObject beforeObj = obj.getJSONObject("before");
                if (beforeObj == null || beforeObj.isEmpty()) {
                    log.warn("删除操作的before数据为空，尝试从source部分获取必要信息");

                    // 尝试从source部分获取表信息，构建基本删除语句
                    JSONObject source = obj.getJSONObject("source");
                    if (source != null && source.containsKey("table")) {
                        String table = source.getString("table");

                        // 尝试从主体获取主键值
                        JSONObject payload = null;
                        if (obj.containsKey("payload")) {
                            payload = obj.getJSONObject("payload");
                        }

                        Map<String, Object> keyMap = extractPrimaryKeyValues(obj, payload);

                        if (!keyMap.isEmpty()) {
                            StringBuilder whereClause = new StringBuilder();
                            for (Map.Entry<String, Object> entry : keyMap.entrySet()) {
                                if (whereClause.length() > 0) {
                                    whereClause.append(" AND ");
                                }

                                String key = entry.getKey();
                                Object val = entry.getValue();

                                if (val instanceof String) {
                                    // 转义单引号
                                    String safeVal = val.toString().replace("'", "''");
                                    whereClause.append("`").append(key).append("`='").append(safeVal).append("'");
                                } else {
                                    whereClause.append("`").append(key).append("`=").append(val);
                                }
                            }

                            sql = "DELETE FROM `" + targetDb + "`.`" + targetTable + "` WHERE " + whereClause;
                            log.info("基于CDC元数据构建的删除语句: {}", sql);
                        } else {
                            log.warn("无法从CDC事件中提取主键信息，无法执行删除操作");
                            return;
                        }
                    } else {
                        log.warn("无法从source部分获取表信息，跳过处理");
                        return;
                    }
                } else {
                    log.info("删除前数据: {}", beforeObj);

                    // 使用主键或所有字段进行删除
                    StringBuilder whereClause = new StringBuilder();

                    // 优先尝试使用id字段
                    String id = beforeObj.getString("id");
                    if (id != null && !id.isEmpty()) {
                        sql = "DELETE FROM `" + targetDb + "`.`" + targetTable + "` WHERE id='" + id.replace("'", "''") + "'";
                    } else {
                        // 如果没有id字段，使用所有非空字段构建条件
                        for (Map.Entry<String, Object> entry : beforeObj.entrySet()) {
                            String key = entry.getKey();
                            Object fieldValue = entry.getValue();

                            if (fieldValue != null) {
                                if (whereClause.length() > 0) {
                                    whereClause.append(" AND ");
                                }

                                if (fieldValue instanceof String) {
                                    // 转义单引号
                                    String safeVal = fieldValue.toString().replace("'", "''");
                                    whereClause.append("`").append(key).append("`='").append(safeVal).append("'");
                                } else {
                                    whereClause.append("`").append(key).append("`=").append(fieldValue);
                                }
                            }
                        }

                        if (whereClause.length() == 0) {
                            log.warn("无法生成有效的WHERE子句，跳过删除操作");
                            return;
                        }

                        sql = "DELETE FROM `" + targetDb + "`.`" + targetTable + "` WHERE " + whereClause;
                    }
                }

                deleteCount.incrementAndGet();
                // 更新表级别计数器
                tableDeleteCount.computeIfAbsent(sourceTable, k -> new AtomicInteger(0)).incrementAndGet();
            } else {
                log.warn("未处理的操作类型: {}", op);
                return;
            }

            // 执行SQL
            if (StringUtils.isNotEmpty(sql)) {
                log.info("执行SQL: {}", sql);
                int result = 0;
                try {
                    // 使用目标数据库连接执行SQL
                    if (targetDbUrl != null && targetDbUsername != null && targetDbPassword != null) {
                        result = DbUtil.insertOrUpdateWithTargetDb(sql, targetDbUrl, targetDbUsername, targetDbPassword);
                    } else {
                        // 如果没有目标数据库连接信息，使用默认数据源
                        log.warn("未配置目标数据库连接信息，使用默认数据源执行SQL");
                        result = DbUtil.insertOrUpdate(sql);
                    }
                    log.info("SQL执行结果: {}, 影响行数: {}", result > 0 ? "成功" : "失败", result);
                } catch (Exception e) {
                    log.error("执行SQL异常: {}，错误: {}", sql, e.getMessage(), e);
                    errorCount.incrementAndGet();
                    // 更新表级别计数器
                    tableErrorCount.computeIfAbsent(sourceTable, k -> new AtomicInteger(0)).incrementAndGet();

                    // 记录同步异常
                    SyncException exception = new SyncException();
                    exception.setTaskId(taskId);
                    exception.setTableName(sourceTable);
                    exception.setErrorMessage("执行SQL异常: " + e.getMessage() + "\nSQL: " + sql);
                    exception.setErrorTime(new Date());

                    // 使用DAO保存异常信息
                    if (syncExceptionDao != null) {
                        try {
                            syncExceptionDao.insert(exception);
                        } catch (Exception ex) {
                            log.error("保存异常记录失败: {}", ex.getMessage(), ex);
                        }
                    }
                }

                if (result <= 0) {
                    log.error("执行SQL失败: {}", sql);
                    errorCount.incrementAndGet();
                    // 更新表级别计数器
                    tableErrorCount.computeIfAbsent(sourceTable, k -> new AtomicInteger(0)).incrementAndGet();

                    // 记录同步异常
                    SyncException exception = new SyncException();
                    exception.setTaskId(taskId);
                    exception.setTableName(sourceTable);
                    exception.setErrorMessage("执行SQL失败: " + sql);
                    exception.setErrorTime(new Date());

                    // 使用DAO保存异常信息
                    if (syncExceptionDao != null) {
                        try {
                            syncExceptionDao.insert(exception);
                        } catch (Exception ex) {
                            log.error("保存异常记录失败: {}", ex.getMessage(), ex);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("处理CDC数据异常: {}, 原始数据: {}", e.getMessage(), value, e);
            errorCount.incrementAndGet();

            // 记录同步异常
            SyncException exception = new SyncException();
            exception.setTaskId(taskId);
            exception.setErrorMessage("处理CDC数据异常: " + e.getMessage() + "\n原始数据: " + value);
            exception.setErrorTime(new Date());

            // 使用DAO保存异常信息
            if (syncExceptionDao != null) {
                try {
                    syncExceptionDao.insert(exception);
                } catch (Exception ex) {
                    log.error("保存异常记录失败: {}", ex.getMessage(), ex);
                }
            }
        }
    }

    /**
     * 启动定期保存统计数据的调度任务
     */
    private void startStatisticsSavingTask() {
        try {
            scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("FlinkCDCSink-StatScheduler-" + taskId);
                return t;
            });
            scheduler.scheduleWithFixedDelay(() -> {
                try {
                    if (closed || scheduler == null || scheduler.isShutdown() || Thread.currentThread().isInterrupted()) {
                        log.info("定时任务线程({})检测到已关闭，直接return并退出线程", Thread.currentThread().getName());
                        throw new RuntimeException("Scheduler closed, exit thread.");
                    }
                    saveAllStatisticsToDatabase();
                } catch (Throwable t) {
                    log.error("定时任务线程({})异常退出: {}", Thread.currentThread().getName(), t.getMessage(), t);
                } finally {
                    if (closed || scheduler == null || scheduler.isShutdown() || Thread.currentThread().isInterrupted()) {
                        log.info("定时任务线程({})已彻底退出", Thread.currentThread().getName());
                    }
                }
            }, 10, 10, java.util.concurrent.TimeUnit.SECONDS);
            log.info("任务[{}]的统计数据保存调度器已启动, 线程: {}", taskId, Thread.currentThread().getName());
        } catch (Exception e) {
            log.error("启动统计数据保存调度器异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 保存所有统计数据到数据库
     */
    private synchronized void saveAllStatisticsToDatabase() {
        if (closed || scheduler == null || scheduler.isShutdown() || Thread.currentThread().isInterrupted()) {
            log.info("saveAllStatisticsToDatabase: 已关闭，线程({})直接return", Thread.currentThread().getName());
            return;
        }
        // 检查是否有表映射配置
        if (StringUtils.isEmpty(tableMapping)) {
            log.warn("任务[{}]没有表映射配置，跳过保存统计数据", taskId);
            return;
        }
        try {
            // 解析表映射关系
            JSONObject mappings = JSONObject.parseObject(tableMapping);
            // 遍历所有表
            for (String sourceTable : mappings.keySet()) {
                // 获取表的统计数据
                int insertCountVal = getInsertCount(sourceTable);
                int updateCountVal = getUpdateCount(sourceTable);
                int deleteCountVal = getDeleteCount(sourceTable);
                int errorCountVal = getErrorCount(sourceTable);
                // 计算总同步数
                int syncCountVal = insertCountVal + updateCountVal + deleteCountVal;
                // 保存或更新统计数据
                saveTableStatistics(sourceTable, syncCountVal, errorCountVal, insertCountVal, updateCountVal, deleteCountVal);
            }
            log.info("已成功保存任务[{}]的所有表统计数据, 线程: {}", taskId, Thread.currentThread().getName());
        } catch (Exception e) {
            log.error("保存所有统计数据异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 保存单个表的统计数据
     */
    private void saveTableStatistics(String tableName, int syncCount, int errorCount,
                                     int insertCount, int updateCount, int deleteCount) {
        if (syncStatisticsDao == null) {
            log.warn("SyncStatisticsDao实例不可用，无法保存表[{}]的统计数据", tableName);
            return;
        }

        try {
            // 查询是否存在该表的统计记录
            SyncStatistics stats = syncStatisticsDao.selectByTaskIdAndTableName(taskId, tableName);

            if (stats == null) {
                // 不存在则创建新记录
                stats = new SyncStatistics();
                stats.setTaskId(taskId);
                stats.setTableName(tableName);
                stats.setSyncCount(syncCount);
                stats.setExceptionCount(errorCount);
                stats.setStartTime(new Date());
                stats.setLastUpdateTime(new Date());

                // 直接设置各种操作的计数
                stats.setInsertCount(insertCount);
                stats.setUpdateCount(updateCount);
                stats.setDeleteCount(deleteCount);

                // 保存额外信息到remark字段
                JSONObject extraInfo = new JSONObject();
                extraInfo.put("lastSyncTime", new Date().getTime());
                stats.setRemark(extraInfo.toJSONString());

                syncStatisticsDao.insert(stats);
                log.info("已创建表[{}]的统计记录: 同步数={}, 插入={}, 更新={}, 删除={}", tableName, syncCount, insertCount, updateCount, deleteCount);
            } else {
                // 存在则更新记录
                stats.setSyncCount(syncCount);
                stats.setExceptionCount(errorCount);
                stats.setLastUpdateTime(new Date());

                // 直接更新各种操作的计数
                stats.setInsertCount(insertCount);
                stats.setUpdateCount(updateCount);
                stats.setDeleteCount(deleteCount);

                // 更新额外信息
                JSONObject extraInfo = new JSONObject();
                try {
                    if (stats.getRemark() != null) {
                        extraInfo = JSONObject.parseObject(stats.getRemark());
                    }
                } catch (Exception e) {
                    log.warn("解析表[{}]额外信息异常，将重新创建", tableName);
                }
                extraInfo.put("lastSyncTime", new Date().getTime());
                stats.setRemark(extraInfo.toJSONString());

                syncStatisticsDao.update(stats);
//                log.info("已更新表[{}]的统计记录: 同步数={}, 插入={}, 更新={}, 删除={}", tableName, syncCount, insertCount, updateCount, deleteCount);
            }
        } catch (Exception e) {
            log.error("保存表[{}]的统计数据异常: {}", tableName, e.getMessage(), e);
        }
    }

    @Override
    public void close() throws Exception {
        log.info(">>> [DEBUG] FlinkCDCSink close() 触发, taskId={}, 线程: {}", taskId, Thread.currentThread().getName());
        closed = true;
        if (scheduler != null && !scheduler.isShutdown()) {
            try {
                log.info("任务[{}]准备关闭，保存最终统计数据", taskId);
                saveAllStatisticsToDatabase();
                scheduler.shutdownNow();
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.warn("调度器未能在10秒内关闭，强制退出");
                }
            } catch (Exception e) {
                log.error("关闭统计数据保存调度器异常: {}", e.getMessage(), e);
            }
            scheduler = null;
        }
        super.close();
        FlinkCDCSink oldInstance = INSTANCES.remove(taskId);
        log.info("从实例映射中移除任务[{}]: {}, 剩余实例: {}", taskId, oldInstance != null ? "成功" : "实例不存在", INSTANCES.keySet());
        log.info("关闭Flink CDC Sink，任务ID：{}", taskId);
    }

    /**
     * 获取目标表名
     *
     * @param sourceTable 源表名
     * @return 目标表名
     */
    private String getTargetTable(String sourceTable) {
        if (StringUtils.isEmpty(tableMapping)) {
            log.info("没有表映射配置，使用源表名作为目标表名: {}", sourceTable);
            return sourceTable; // 没有映射则默认使用相同表名
        }

        try {
            JSONObject mappings = JSONObject.parseObject(tableMapping);
            String targetTable = mappings.getString(sourceTable);
            log.info("表映射结果: {}=>{}", sourceTable, targetTable);

            if (StringUtils.isEmpty(targetTable)) {
                log.info("在映射中未找到源表[{}]，使用源表名作为目标表名", sourceTable);
                return sourceTable;
            }

            return targetTable;
        } catch (Exception e) {
            log.error("解析表映射关系异常", e);
            return sourceTable;
        }
    }

    /**
     * 获取任务的实例
     *
     * @param taskId 任务ID
     * @return FlinkCDCSink实例，不存在则返回null
     */
    public static FlinkCDCSink getInstance(Integer taskId) {
        return INSTANCES.get(taskId);
    }

    /**
     * 获取插入计数
     */
    public int getInsertCount() {
        return insertCount.get();
    }

    /**
     * 获取更新计数
     */
    public int getUpdateCount() {
        return updateCount.get();
    }

    /**
     * 获取删除计数
     */
    public int getDeleteCount() {
        return deleteCount.get();
    }

    /**
     * 获取错误计数
     */
    public int getErrorCount() {
        return errorCount.get();
    }

    /**
     * 获取指定表的插入计数
     *
     * @param tableName 表名
     * @return 插入记录数
     */
    public int getInsertCount(String tableName) {
        AtomicInteger counter = tableInsertCount.get(tableName);
        return counter != null ? counter.get() : 0;
    }

    /**
     * 获取指定表的更新计数
     *
     * @param tableName 表名
     * @return 更新记录数
     */
    public int getUpdateCount(String tableName) {
        AtomicInteger counter = tableUpdateCount.get(tableName);
        return counter != null ? counter.get() : 0;
    }

    /**
     * 获取指定表的删除计数
     *
     * @param tableName 表名
     * @return 删除记录数
     */
    public int getDeleteCount(String tableName) {
        AtomicInteger counter = tableDeleteCount.get(tableName);
        return counter != null ? counter.get() : 0;
    }

    /**
     * 获取指定表的错误计数
     *
     * @param tableName 表名
     * @return 错误记录数
     */
    public int getErrorCount(String tableName) {
        AtomicInteger counter = tableErrorCount.get(tableName);
        return counter != null ? counter.get() : 0;
    }

    /**
     * 尝试从CDC事件中提取主键值
     *
     * @param obj CDC事件对象
     * @param payload 可选的payload对象
     * @return 主键名和值的映射
     */
    private Map<String, Object> extractPrimaryKeyValues(JSONObject obj, JSONObject payload) {
        Map<String, Object> keyMap = new HashMap<>();

        // 方法1：尝试从主键字段获取
        if (obj.containsKey("key")) {
            Object keyObj = obj.get("key");
            if (keyObj instanceof JSONObject) {
                JSONObject key = (JSONObject) keyObj;
                if (!key.isEmpty()) {
                    return key;
                }
            }
        }

        // 方法2：尝试从payload中的primaryKey部分获取
        if (payload != null && payload.containsKey("primaryKey")) {
            Object pkObj = payload.get("primaryKey");
            if (pkObj instanceof JSONObject) {
                JSONObject pk = (JSONObject) pkObj;
                if (!pk.isEmpty()) {
                    return pk;
                }
            }
        }

        // 方法3：查找source中的主键信息
        if (obj.containsKey("source")) {
            JSONObject source = obj.getJSONObject("source");
            if (source.containsKey("primary_keys")) {
                Object pksObj = source.get("primary_keys");
                if (pksObj instanceof JSONArray) {
                    JSONArray pks = (JSONArray) pksObj;
                    // 如果有主键但没有值，尝试获取主键字段名
                    for (int i = 0; i < pks.size(); i++) {
                        String pkName = pks.getString(i);
                        // 尝试从after或before中获取这个字段的值
                        Object pkValue = null;

                        if (obj.containsKey("after")) {
                            JSONObject after = obj.getJSONObject("after");
                            if (after != null && after.containsKey(pkName)) {
                                pkValue = after.get(pkName);
                            }
                        }

                        if (pkValue == null && obj.containsKey("before")) {
                            JSONObject before = obj.getJSONObject("before");
                            if (before != null && before.containsKey(pkName)) {
                                pkValue = before.get(pkName);
                            }
                        }

                        if (pkValue != null) {
                            keyMap.put(pkName, pkValue);
                        }
                    }
                }
            }
        }

        return keyMap;
    }
} 