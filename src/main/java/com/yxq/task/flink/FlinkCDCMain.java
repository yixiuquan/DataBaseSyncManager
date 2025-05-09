package com.yxq.task.flink;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.table.StartupOptions;
import com.yxq.task.entity.Database;
import com.yxq.task.entity.SyncTask;
import com.yxq.task.util.AESUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.core.execution.JobClient;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Flink CDC 主程序类
 * 用于启动和管理Flink CDC任务
 */
@Slf4j
@Component
public class FlinkCDCMain {

    // 存储正在运行的Flink作业
    private static final Map<Integer, RunningJobInfo> RUNNING_JOBS = new ConcurrentHashMap<>();

    // 新增：用于保存env和JobID
    public static class RunningJobInfo {
        private final StreamExecutionEnvironment env;
        private final org.apache.flink.api.common.JobID jobId;

        public RunningJobInfo(StreamExecutionEnvironment env, org.apache.flink.api.common.JobID jobId) {
            this.env = env;
            this.jobId = jobId;
        }

        public StreamExecutionEnvironment getEnv() {
            return env;
        }

        public org.apache.flink.api.common.JobID getJobId() {
            return jobId;
        }
    }

    /**
     * 启动同步任务
     *
     * @param syncTask 同步任务配置
     * @param sourceDb 源数据库配置
     * @param targetDb 目标数据库配置
     * @return 是否成功启动
     */
    public boolean startSyncTask(SyncTask syncTask, Database sourceDb, Database targetDb) {
        try {
            log.info("开始启动同步任务，任务信息：{}, 源数据库：{}({}), 目标数据库：{}({})",
                    syncTask.getTaskName(),
                    sourceDb.getDbName(),
                    sourceDb.getHost() + ":" + sourceDb.getPort(),
                    targetDb.getDbName(),
                    targetDb.getHost() + ":" + targetDb.getPort());

            // 检查任务是否已经在运行
            if (RUNNING_JOBS.containsKey(syncTask.getId())) {
                log.warn("任务[{}]已经在运行中", syncTask.getTaskName());
                return true;
            }

            // 解密密码
            String password = AESUtil.decrypt(sourceDb.getPassword());

            // 执行MySQL CDC测试和诊断
            log.info("执行MySQL CDC测试和诊断...");
            testBinlogCapture(sourceDb);

            // 创建Flink执行环境
            StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

            // 【修复点1】增强Flink环境设置
            env.setParallelism(1);
            // 禁用操作链接，提高稳定性
            env.disableOperatorChaining();
            // 启用检查点，提高任务稳定性和容错能力
            env.enableCheckpointing(60000); // 每60秒做一次检查点
            env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
            env.getCheckpointConfig().setMinPauseBetweenCheckpoints(30000); // 两次检查点之间至少间隔30秒
            env.getCheckpointConfig().setCheckpointTimeout(120000); // 检查点超时时间2分钟
            env.getCheckpointConfig().setTolerableCheckpointFailureNumber(3); // 允许连续失败3次
            // 设置重启策略，遇到失败时自动重启
            env.setRestartStrategy(RestartStrategies.fixedDelayRestart(3, 10000));

            log.info("Flink执行环境创建成功，并行度：1，已启用检查点和重启策略");

            // 解析表配置
            String tablesStr = syncTask.getTables();
            log.info("表配置字符串: {}", tablesStr);

            List<Map<String, String>> tableConfigs = new ArrayList<>();

            // 检查是否是JSON格式
            if (tablesStr != null && !tablesStr.isEmpty()) {
                if (tablesStr.trim().startsWith("[")) {
                    // JSON数组格式
                    try {
                        log.info("使用JSON格式解析表配置");
                        tableConfigs = JSON.parseObject(tablesStr, new TypeReference<List<Map<String, String>>>() {
                        });
                    } catch (Exception e) {
                        log.error("解析表配置JSON异常: {}", e.getMessage(), e);
                        return false;
                    }
                } else {
                    // 逗号分隔的字符串格式
                    log.info("使用逗号分隔格式解析表配置");
                    String[] tableNames = tablesStr.split(",");
                    for (String tableName : tableNames) {
                        if (!tableName.trim().isEmpty()) {
                            Map<String, String> config = new HashMap<>();
                            config.put("sourceTable", tableName.trim());
                            config.put("targetTable", tableName.trim()); // 默认使用相同表名
                            tableConfigs.add(config);
                        }
                    }
                }
            }

            if (tableConfigs.isEmpty()) {
                log.error("任务[{}]启动失败：表配置为空", syncTask.getTaskName());
                return false;
            }

            log.info("解析到 {} 个表配置", tableConfigs.size());

            // 构建表映射关系
            Map<String, String> tableMappings = new HashMap<>();
            List<String> includeTables = new ArrayList<>();

            for (Map<String, String> config : tableConfigs) {
                String sourceTable = config.get("sourceTable");
                String targetTable = config.get("targetTable");

                if (sourceTable != null && targetTable != null) {
                    log.info("表映射: {} => {}", sourceTable, targetTable);
                    tableMappings.put(sourceTable, targetTable);

                    // 添加到CDC监控表列表，格式为 dbName.tableName
                    String fullTableName = sourceDb.getDbName() + "." + sourceTable;
                    includeTables.add(fullTableName);
                    log.info("监控表添加: {}", fullTableName);
                }
            }

            // 将表映射关系转为JSON字符串
            String tableMappingJson = JSON.toJSONString(tableMappings);
            log.info("表映射JSON: {}", tableMappingJson);

            log.info("准备构建MySqlSource，监控表列表: {}", includeTables);
            log.info("同步表名: {}", includeTables.toArray(new String[0]));

            // 构建MySqlSource
            StartupOptions startupOptions = getStartupOptions(syncTask);

            // 显式输出调试信息
            log.info("使用StartupOptions: {}", startupOptions);

            // 配置JDBC超时和保活参数
            Properties jdbcProperties = createJdbcProperties(sourceDb);
            // 补全关键兼容性参数
            log.info("JDBC连接参数: {}", jdbcProperties);

            // 配置binlog消费参数
            Properties debeziumProperties = getDebeziumProperties();
            log.info("Debezium配置参数: {}", debeziumProperties);

            // 构建MySQLSource
            MySqlSource<String> mySqlSource = MySqlSource.<String>builder()
                    .jdbcProperties(jdbcProperties)
                    .hostname(sourceDb.getHost())
                    .port(sourceDb.getPort())
                    .username(sourceDb.getUsername())
                    .password(password)
                    .serverTimeZone("Asia/Shanghai")
                    .databaseList(sourceDb.getDbName())
                    .tableList(includeTables.toArray(new String[0]))
                    //initial:  模式会先做全量快照，直接 select 全表数据 不通过binlog，会导致时间格式数据处理错误，当前版本不支持，因此不用initial
                    //Specific Offset：适合任务失败后需要从中断点恢复的场景。
                    //Latest Offset：适合实时数据处理，关注最新变更的场景。
                    //Earliest Offset：适合需要捕获所有历史变更数据的场景。
                    //Timestamp：适合需要基于特定时间点进行数据快照的场景
                    .startupOptions(StartupOptions.earliest())
                    .deserializer(new CustomDeserialization()) // 使用自定义反序列化
                    .debeziumProperties(debeziumProperties) // 使用简化的Debezium配置
                    .build();

            log.info("MySqlSource构建完成. 监控表: {}", String.join(",", includeTables));
            log.info("数据源配置: {}", JSON.toJSONString(mySqlSource));

            // 创建数据流
            DataStreamSource<String> dataStream = env.fromSource(
                    mySqlSource,
                    WatermarkStrategy.noWatermarks(),
                    "MySQL CDC Source - Task " + syncTask.getId()
            );

            log.info("数据流创建成功，准备添加Sink处理");
            log.info("数据源流：{}", JSON.toJSONString(dataStream.getExecutionConfig()));

            // 添加数据处理Sink
            FlinkCDCSink cdcSink = new FlinkCDCSink(
                    syncTask.getId(),
                    targetDb.getDbName(), targetDb.getHost() + ":" + targetDb.getPort(),
                    tableMappingJson
            );

            DataStreamSink<String> dataStreamSink = dataStream.addSink(cdcSink);
            log.info("Sink处理添加成功，准备异步执行Flink作业");

            // 异步执行Flink作业
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        log.info("开始执行Flink作业，任务ID: {}", syncTask.getId());
                        // 添加JVM关闭钩子，确保在JVM关闭时清理资源
                        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    log.info("JVM关闭钩子触发，清理任务[{}]资源", syncTask.getId());
                                    cleanupTaskResources(syncTask.getId());
                                } catch (Exception e) {
                                    log.error("JVM关闭钩子中清理资源异常: {}", e.getMessage(), e);
                                }
                            }
                        }));
                        JobClient jobClient = env.executeAsync("Database Sync Task " + syncTask.getId());
                        JobID jobId = jobClient.getJobID();
                        log.info("Flink作业已提交，任务ID: {}, JobId:{}", syncTask.getId(), jobId);
                        RUNNING_JOBS.put(syncTask.getId(), new RunningJobInfo(env, jobId));
                    } catch (Exception e) {
                        log.error("Flink作业执行异常，任务ID: {}", syncTask.getId(), e);
                    }
                }
            }).start();
            log.info("任务[{}]启动成功，当前运行任务数：{}", syncTask.getTaskName(), RUNNING_JOBS.size());

            return true;
        } catch (Exception e) {
            log.error("启动同步任务[{}]异常: {}", syncTask.getTaskName(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 停止同步任务
     *
     * @param taskId 任务ID
     * @return 是否成功停止
     */
    public static boolean stopSyncTask(Integer taskId) {
        try {
            log.info("准备停止任务，ID: {}", taskId);
            RunningJobInfo jobInfo = RUNNING_JOBS.get(taskId);
            if (jobInfo == null) {
                log.warn("任务[{}]未在运行中", taskId);
                return true;
            }
            // 本地模式下只能关闭环境，无法通过JobID精确kill
            try {
                jobInfo.getEnv().close();
                log.info("本地模式已关闭Flink环境，任务ID: {}", taskId);
            } catch (Exception e) {
                log.warn("关闭本地Flink环境异常，任务ID: {}, 错误: {}", taskId, e.getMessage());
            }
            RUNNING_JOBS.remove(taskId);
            log.info("已停止任务[{}]，当前运行任务数：{}", taskId, RUNNING_JOBS.size());
            return true;
        } catch (Exception e) {
            log.error("停止任务[{}]异常: {}", taskId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取任务启动选项
     *
     * @param syncTask 同步任务
     * @return 启动选项
     */
    private static StartupOptions getStartupOptions(SyncTask syncTask) {
        // 默认使用initial模式，进行全量+增量同步
        StartupOptions options = StartupOptions.initial();
        log.info("默认使用initial启动选项（进行全量+增量同步）");

        // 获取任务的启动选项配置
        if (syncTask.getStartupOptions() != null && !syncTask.getStartupOptions().isEmpty()) {
            try {
                log.info("解析启动选项配置: {}", syncTask.getStartupOptions());
                Map<String, Object> startupConfig = JSON.parseObject(
                        syncTask.getStartupOptions(),
                        new TypeReference<Map<String, Object>>() {
                        }
                );

                String type = (String) startupConfig.get("type");
                log.info("启动选项类型: {}", type);

                // 【修复点6】强制使用initial模式确保数据完整性
                if (!"initial".equalsIgnoreCase(type)) {
                    log.info("为确保数据同步完整性，强制使用initial模式替代{}模式", type);
                    return StartupOptions.initial();
                }

                if ("initial".equalsIgnoreCase(type)) {
                    // 初始快照
                    log.info("使用initial启动选项");
                    options = StartupOptions.initial();
                } else if ("earliest".equalsIgnoreCase(type)) {
                    // 最早的binlog
                    log.info("使用earliest启动选项");
                    options = StartupOptions.earliest();
                } else if ("latest".equalsIgnoreCase(type)) {
                    // 最新的binlog
                    log.info("使用latest启动选项");
                    options = StartupOptions.latest();
                } else if ("timestamp".equalsIgnoreCase(type)) {
                    // 指定时间戳
                    Long timestamp = (Long) startupConfig.get("timestamp");
                    if (timestamp != null) {
                        log.info("使用timestamp启动选项, 时间戳: {}", timestamp);
                        options = StartupOptions.timestamp(timestamp);
                    }
                } else {
                    // 未识别的类型，默认使用initial
                    log.info("未识别的启动选项类型[{}]，使用默认initial模式", type);
                    options = StartupOptions.initial();
                }
            } catch (Exception e) {
                log.error("解析启动选项配置异常，使用默认initial模式: {}", e.getMessage(), e);
                options = StartupOptions.initial();
            }
        }

        log.info("最终使用的启动模式: {}", JSON.toJSONString(options));
        return options;
    }

    /**
     * 检查任务是否在运行
     *
     * @param taskId 任务ID
     * @return 是否在运行
     */
    public static boolean isTaskRunning(Integer taskId) {
        boolean running = RUNNING_JOBS.containsKey(taskId);
        log.debug("检查任务[{}]是否运行: {}", taskId, running);
        return running;
    }

    /**
     * 获取所有运行中的任务ID
     *
     * @return 运行中的任务ID列表
     */
    public static List<Integer> getRunningTaskIds() {
        return new ArrayList<>(RUNNING_JOBS.keySet());
    }

    /**
     * 获取Debezium配置
     *
     * @return Debezium配置属性
     */
    private static Properties getDebeziumProperties() {
        Properties props = new Properties();

        // ===== 核心配置 - 使用最小配置集确保兼容性 =====

        // 基本服务器标识
        props.setProperty("database.server.id", String.valueOf(new Random().nextInt(9000) + 1000));
        props.setProperty("database.server.name", "mysql-cdc-source");

        // 连接设置
        props.setProperty("database.history.store.only.monitored.tables.ddl", "true");

        // ===== 解决删除操作问题的关键配置 =====
        // 设置删除处理
        props.setProperty("tombstones.on.delete", "false");
        props.setProperty("handle.delete.events", "true");
        props.setProperty("binlog.include.before.after.values", "true");

        // ===== 设置心跳机制，保持连接活跃 =====
        props.setProperty("heartbeat.interval.ms", "2000");
        props.setProperty("connect.timeout.ms", "30000");
        props.setProperty("connect.max.attempts", "3");

        // ===== 改进连接稳定性和事务处理 =====
        props.setProperty("connect.keep.alive", "true");
        props.setProperty("max.batch.size", "2048");
        props.setProperty("max.queue.size", "8192");

        // ===== 数据处理方式 - 不使用transforms避免删除操作问题 =====
        // props.setProperty("transforms", "unwrap");
        // props.setProperty("transforms.unwrap.type", "io.debezium.transforms.ExtractNewRecordState");
        // props.setProperty("transforms.unwrap.drop.tombstones", "false");

        // ===== 事件转换设置 =====
        props.setProperty("event.deserialization.failure.handling.mode", "warn");
        props.setProperty("inconsistent.schema.handling.mode", "warn");

        props.setProperty("debezium.zero.dates.convert.to.null", "true");
        props.setProperty("debezium.errors.tolerance", "all"); //跳过无法解析的记录
        props.setProperty("debezium.errors.log.enable", "true"); // 记录错误日志

        props.setProperty("debezium.converters", "date");
        props.setProperty("debezium.date.format", "yyyy-MM-dd"); // 强制指定日期格式
        // 在原有配置基础上增加：
        props.setProperty("debezium.sanitize.field.names", "true");
        props.setProperty("debezium.skip.messages.without.change", "true");
        props.setProperty("debezium.binary.handling.mode", "hex"); // 调试二进制数据
        props.setProperty("column.propagate.source.type", ".*");  // 强制传递原始类型
        // 使用 initial 模式重新初始化（确保干净状态）
        props.setProperty("snapshot.mode", "initial");
        // 增加快照锁超时配置
        props.setProperty("snapshot.locking.timeout.ms", "10000");
        return props;
    }

    /**
     * 创建优化的JDBC属性
     * @return JDBC连接属性
     */
    private static Properties createJdbcProperties(Database sourceDb) {
        Properties props = new Properties();
        props.setProperty("useSSL", "false");
        props.setProperty("characterEncoding", "UTF-8");
        props.setProperty("useUnicode", "true");
        props.setProperty("zeroDateTimeBehavior", "convertToNull");
        props.setProperty("useLegacyDatetimeCode", "true");
        props.setProperty("tinyInt1isBit", "false");
        props.setProperty("allowPublicKeyRetrieval", "true");
        props.setProperty("connectTimeout", "180000");
        props.setProperty("socketTimeout", "180000");
        props.setProperty("autoReconnect", "true");
        props.setProperty("maxReconnects", "5");
        props.setProperty("tcpKeepAlive", "true");
        props.setProperty("serverTimezone", "Asia/Shanghai");

        // 关键配置：启用binlog功能
        props.setProperty("enabledTLSProtocols", "TLSv1,TLSv1.1,TLSv1.2");
        props.setProperty("useCursorFetch", "true");
        props.setProperty("rewriteBatchedStatements", "true");
        props.setProperty("useReadAheadInput", "false");
        props.setProperty("cacheServerConfiguration", "true");
        props.setProperty("useLegacyDatetimeCode", "true");

        //如果有额外参数，添加到属性中
        if (sourceDb.getParam() != null && !sourceDb.getParam().isEmpty()) {
            log.info("添加源数据库额外参数: {}", sourceDb.getParam());
            String[] params = sourceDb.getParam().split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    if (StringUtils.isEmpty(props.getProperty(keyValue[0]))
                            || (StringUtils.isNotEmpty(keyValue[1]) && !keyValue[1].equals(props.getProperty(keyValue[0])))) {
                        props.setProperty(keyValue[0], keyValue[1]);
                        log.info("JDBC参数: {}={}", keyValue[0], keyValue[1]);
                    }
                }
            }
        }

        return props;
    }

    /**
     * MySQL CDC连接诊断方法
     * 检查服务器binlog配置和权限
     */
    private static void checkMysqlBinlogConfig(String host, int port, String username, String password, String param) {
        java.sql.Connection conn = null;
        java.sql.PreparedStatement pst = null;
        java.sql.ResultSet rs = null;
        try {
            // 构建连接
            param = StringUtils.isNotEmpty(param) ? param : "useUnicode=true&characterEncoding=UTF-8&allowMultiQueries=true&useSSL=false";
            String url = "jdbc:mysql://" + host + ":" + port + "?" + param + "&zeroDateTimeBehavior=convertToNull&useLegacyDatetimeCode=true";
            log.info("诊断MySQL连接: {}", url);
            conn = java.sql.DriverManager.getConnection(url, username, password);

            // 检查binlog是否启用
            log.info("检查MySQL binlog配置...");
            pst = conn.prepareStatement("SHOW VARIABLES LIKE 'log_bin'");
            rs = pst.executeQuery();
            boolean binlogEnabled = false;
            if (rs.next()) {
                binlogEnabled = "ON".equalsIgnoreCase(rs.getString(2));
                log.info("Binlog状态: {}", rs.getString(2));
            }
            rs.close();
            pst.close();

            // 检查binlog格式
            pst = conn.prepareStatement("SHOW VARIABLES LIKE 'binlog_format'");
            rs = pst.executeQuery();
            boolean correctFormat = false;
            if (rs.next()) {
                correctFormat = "ROW".equalsIgnoreCase(rs.getString(2));
                log.info("Binlog格式: {}", rs.getString(2));
                if (!correctFormat) {
                    log.error("【严重问题】Binlog格式必须是ROW，当前是: {}", rs.getString(2));
                }
            }
            rs.close();
            pst.close();

            // 检查binlog保留期
            pst = conn.prepareStatement("SHOW VARIABLES LIKE 'binlog_expire_logs_seconds'");
            rs = pst.executeQuery();
            if (rs.next()) {
                log.info("Binlog保留期(秒): {}", rs.getString(2));
            }
            rs.close();
            pst.close();

            // 检查用户权限
            log.info("检查用户CDC权限...");
            pst = conn.prepareStatement("SHOW GRANTS FOR CURRENT_USER");
            rs = pst.executeQuery();
            boolean hasReplicationPermission = false;
            while (rs.next()) {
                String grant = rs.getString(1);
                log.info("用户权限: {}", grant);
                if (grant.contains("REPLICATION") || grant.contains("ALL PRIVILEGES")) {
                    hasReplicationPermission = true;
                }
            }

            boolean hasProblems = false;

            if (!hasReplicationPermission) {
                hasProblems = true;
                log.error("【严重问题】当前用户[{}]缺少REPLICATION SLAVE权限，CDC无法正常工作", username);
            }

            if (!binlogEnabled) {
                hasProblems = true;
                log.error("【严重问题】MySQL binlog未启用，CDC无法工作");
            }

            if (!correctFormat) {
                hasProblems = true;
                log.error("【严重问题】MySQL binlog格式不是ROW，CDC无法正确工作");
            }

            // 如果有问题，生成修复脚本
            if (hasProblems) {
                log.error("=== 发现CDC配置问题，无法捕获增量变更 ===");
                generateFixScript(host, port, username);
            } else {
                log.info("MySQL binlog配置正常，CDC应该能够正常工作");
            }

        } catch (Exception e) {
            log.error("检查MySQL配置异常", e);
            // 如果异常，也生成修复脚本
            generateFixScript(host, port, username);
        } finally {
            try {
                if (rs != null) rs.close();
                if (pst != null) pst.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                log.error("关闭资源异常", e);
            }
        }
    }

    /**
     * 生成修复脚本
     * 用于帮助用户解决MySQL配置问题
     * @param host MySQL主机
     * @param port MySQL端口
     * @param user MySQL用户名
     */
    private static void generateFixScript(String host, int port, String user) {
        StringBuilder script = new StringBuilder();
        script.append("\n========== MySQL配置修复脚本 ==========\n");
        script.append("# 请使用管理员权限执行以下命令修复MySQL配置\n\n");

        // 1. 修改my.cnf配置
        script.append("===== 步骤1: 修改MySQL配置文件 =====\n");
        script.append("# 编辑 my.cnf (通常位于 /etc/my.cnf 或 /etc/mysql/my.cnf):\n");
        script.append("# 在 [mysqld] 部分添加或修改以下配置:\n\n");
        script.append("log_bin = mysql-bin\n");
        script.append("binlog_format = ROW\n");
        script.append("server_id = 1\n");
        script.append("binlog_do_db = ").append(user).append("\n");
        script.append("binlog_expire_logs_seconds = 604800\n\n");
        script.append("# 保存文件后重启MySQL服务\n");
        script.append("systemctl restart mysqld\n\n");

        // 2. 授予用户权限
        script.append("===== 步骤2: 授予用户必要权限 =====\n");
        script.append("# 登录MySQL并执行:\n\n");
        script.append("mysql -u root -p\n\n");
        script.append("# 然后执行以下SQL:\n");
        script.append("GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO '").append(user).append("'@'%';\n");
        script.append("FLUSH PRIVILEGES;\n\n");

        // 3. 设置MySQL全局变量
        script.append("===== 步骤3: 设置MySQL全局变量 =====\n");
        script.append("# 登录MySQL并执行:\n\n");
        script.append("SET GLOBAL binlog_format = 'ROW';\n");
        script.append("SET GLOBAL log_bin = ON;\n\n");

        // 4. 验证配置
        script.append("===== 步骤4: 验证配置 =====\n");
        script.append("# 检查binlog是否开启:\n");
        script.append("SHOW VARIABLES LIKE 'log_bin';\n\n");
        script.append("# 检查binlog格式:\n");
        script.append("SHOW VARIABLES LIKE 'binlog_format';\n\n");
        script.append("# 检查用户权限:\n");
        script.append("SHOW GRANTS FOR '").append(user).append("'@'%';\n\n");

        // 5. 重启CDC任务
        script.append("===== 步骤5: 重启CDC同步任务 =====\n");
        script.append("# 在完成上述步骤后，重启您的CDC同步任务\n\n");

        log.error(script.toString());
    }

    /**
     * MySQL Binlog测试方法
     * 直接测试通过binlog模式获取变更
     * @param sourceDb 源数据库
     */
    public static void testBinlogCapture(Database sourceDb) {
        try {
            String password = AESUtil.decrypt(sourceDb.getPassword());

            // 输出测试信息
            log.info("执行MySQL Binlog连接测试...");
            log.info("数据库信息: {}:{}/{}",
                    sourceDb.getHost(),
                    sourceDb.getPort(),
                    sourceDb.getDbName());

            // 先检查binlog配置
            checkMysqlBinlogConfig(sourceDb.getHost(), sourceDb.getPort(), sourceDb.getUsername(), password, sourceDb.getParam());

            // 在数据库中创建测试表并插入测试数据
            java.sql.Connection conn = null;
            java.sql.Statement stmt = null;

            try {
                // 创建连接
                String param = StringUtils.isNotEmpty(sourceDb.getParam()) ? sourceDb.getParam() : "useUnicode=true&characterEncoding=UTF-8&allowMultiQueries=true&useSSL=false";
                String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?" + param + "&zeroDateTimeBehavior=convertToNull&useLegacyDatetimeCode=true",
                        sourceDb.getHost(),
                        sourceDb.getPort(),
                        sourceDb.getDbName());
                log.info("连接到-源-数据库: {}", jdbcUrl);
                conn = java.sql.DriverManager.getConnection(jdbcUrl, sourceDb.getUsername(), password);
                stmt = conn.createStatement();

                // 尝试创建测试表
                String createTableSql = "CREATE TABLE IF NOT EXISTS cdc_test_table (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "name VARCHAR(50), " +
                        "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "date_time datetime DEFAULT CURRENT_TIMESTAMP)";
                boolean execute = stmt.execute(createTableSql);
                if (execute) {
                    log.info("测试表创建成功: cdc_test_table");
                } else {
                    log.info("测试表已存在: cdc_test_table");
                }

                // 插入测试数据
                String insertSql = "INSERT INTO cdc_test_table (name) VALUES ('CDC Test " + System.currentTimeMillis() + "')";
                int insertCount = stmt.executeUpdate(insertSql);
                if (insertCount > 0) {
                    log.info("成功插入源库测试数据");
                } else {
                    log.info("插入源库测试数据失败");
                }

                // 告知用户如何验证
                log.info("===== CDC测试指南 =====");
                log.info("1. 已在数据库 {} 中创建测试表 cdc_test_table 并插入数据", sourceDb.getDbName());
                log.info("2. 正在启动CDC同步任务...");
                log.info("3. 启动后，请手动在MySQL中执行以下SQL进行测试:");
                log.info("   INSERT INTO cdc_test_table (name) VALUES ('Test Insert')");
                log.info("   UPDATE cdc_test_table SET name = 'Test Update' WHERE name LIKE 'Test%' LIMIT 1");
                log.info("   DELETE FROM cdc_test_table WHERE name LIKE 'Test%' LIMIT 1");
                log.info("4. 观察CDC日志是否捕获了这些变更");
                log.info("5. 如果没有捕获，请执行修复脚本并重试");

            } finally {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }

        } catch (Exception e) {
            log.error("Binlog测试异常", e);
        }
    }

    /**
     * 清理任务资源
     * 确保在任务结束时正确清理所有资源
     *
     * @param taskId 任务ID
     */
    private static void cleanupTaskResources(Integer taskId) {
        try {
            log.info("清理任务[{}]资源", taskId);

            // 从运行中的作业映射中移除
            RUNNING_JOBS.remove(taskId);

            // 尝试从FlinkCDCSink类中获取实例并移除
            try {
                java.lang.reflect.Field instancesField = FlinkCDCSink.class.getDeclaredField("INSTANCES");
                instancesField.setAccessible(true);
                Object instancesObj = instancesField.get(null);

                if (instancesObj != null && instancesObj instanceof ConcurrentHashMap) {
                    @SuppressWarnings("unchecked")
                    ConcurrentHashMap<Integer, FlinkCDCSink> instances =
                            (ConcurrentHashMap<Integer, FlinkCDCSink>) instancesObj;

                    if (instances.containsKey(taskId)) {
                        FlinkCDCSink sinkInstance = instances.get(taskId);
                        log.info("找到任务[{}]的CDC Sink实例，尝试关闭", taskId);

                        if (sinkInstance != null) {
                            try {
                                // 调用 close 方法确保资源释放
                                sinkInstance.close();
                                log.info("成功关闭任务[{}]的CDC Sink实例", taskId);
                            } catch (Exception e) {
                                log.error("关闭任务[{}]的CDC Sink实例异常: {}", taskId, e.getMessage(), e);
                                // 强制移除实例
                                instances.remove(taskId);
                                log.info("强制移除任务[{}]的CDC Sink实例", taskId);
                            }
                        } else {
                            // 实例为null但在映射中存在，直接移除
                            instances.remove(taskId);
                            log.info("强制移除任务[{}]的空CDC Sink实例引用", taskId);
                        }
                    } else {
                        log.info("任务[{}]的CDC Sink实例不存在", taskId);
                    }
                }
            } catch (Exception e) {
                log.warn("获取和清理CDC Sink实例异常: {}", e.getMessage());
            }

            log.info("任务[{}]资源清理完成", taskId);
        } catch (Exception e) {
            log.error("清理任务[{}]资源时发生异常: {}", taskId, e.getMessage(), e);
        }
    }
} 