package com.yxq.task.flink;

import com.alibaba.fastjson.JSONObject;
import com.ververica.cdc.debezium.DebeziumDeserializationSchema;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.util.Collector;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;

import java.util.List;
import java.util.Map;

/**
 * 自定义的CDC反序列化实现
 * 用于将Debezium的CDC事件转换为JSON字符串
 */
@Slf4j
public class CustomDeserialization implements DebeziumDeserializationSchema<String> {

    /**
     * 反序列化方法，将Debezium的SourceRecord转换为JSON字符串
     *
     * @param sourceRecord Debezium的SourceRecord
     * @param collector 输出收集器
     */
    @Override
    public void deserialize(SourceRecord sourceRecord, Collector<String> collector) throws Exception {
        try {
            String topic = sourceRecord.topic();
            log.info("处理CDC事件，主题: {}", topic);
            
            // 获取源数据字段
            Struct value = (Struct) sourceRecord.value();
            
            // 打印完整原始记录信息用于调试
            log.info("CDC原始记录: key={}, value={}", sourceRecord.key(), value);
            
            // 检查值是否为空
            if (value == null) {
                log.warn("收到空CDC值，跳过处理");
                return;
            }
            
            // 处理特殊消息类型
            if (value.schema() != null && value.schema().name() != null && 
                value.schema().name().equals("io.debezium.connector.mysql.SchemaChangeValue")) {
                log.info("收到Schema变更事件: {}", value);
                // 处理Schema变更事件
                processSchemaChange(value, collector);
                return;
            }
            
            // 获取操作类型
            String op = value.getString("op"); // c:增 d:删 u:改 r:查询
            
            // 特别处理删除操作
            if ("d".equals(op)) {
                log.info("【重要】收到删除操作: {}", value);
            }
            
            // 输出操作类型用于调试
            log.info("CDC操作类型: {}", op);
            
            // 解析操作前后的数据
            JSONObject data = new JSONObject();
            
            // 解析source信息
            Struct source = value.getStruct("source");
            if (source != null) {
                // 提取源信息
                String db = source.getString("db");
                String table = source.getString("table");
                Long ts = source.getInt64("ts_ms");
                
                data.put("db", db);
                data.put("tableName", table);
                data.put("ts", ts);
                data.put("op", op);
                
                log.info("CDC事件源信息: db={}, table={}, ts={}, op={}", db, table, ts, op);
            } else {
                log.warn("CDC事件缺少source信息");
            }
            
            // 解析变更前数据
            Struct before = value.getStruct("before");
            JSONObject beforeJson = new JSONObject();
            if (before != null) {
                beforeJson = convertStruct(before);
                data.put("before", beforeJson);
                log.info("变更前数据: {}", beforeJson);
            } else if ("d".equals(op)) {
                // 对于删除操作，如果before为空，这是个严重问题
                log.error("【严重错误】删除操作但没有before数据: {}", value);
            }
            
            // 解析变更后数据
            Struct after = value.getStruct("after");
            JSONObject afterJson = new JSONObject();
            if (after != null) {
                afterJson = convertStruct(after);
                data.put("after", afterJson);
                log.info("变更后数据: {}", afterJson);
            }
            
            // 发现缺少字段则记录日志
            if (source == null || (op.equals("u") && (before == null || after == null)) 
                || (op.equals("d") && before == null) 
                || ((op.equals("c") || op.equals("r")) && after == null)) {
                log.warn("CDC事件缺少必要字段: op={}, source={}, before={}, after={}", 
                         op, source != null, before != null, after != null);
                log.warn("原始CDC记录: {}", value);
            }
            
            // 收集数据
            String jsonStr = data.toJSONString();
            log.info("CDC反序列化结果: {}", jsonStr);
            collector.collect(jsonStr);
        } catch (Exception e) {
            log.error("CDC反序列化异常: {}", e.getMessage(), e);
            // 记录异常堆栈信息
            log.error("异常堆栈:", e);
            throw e;
        }
    }

    /**
     * 处理Schema变更事件
     */
    private void processSchemaChange(Struct value, Collector<String> collector) {
        try {
            JSONObject schemaChange = new JSONObject();
            schemaChange.put("type", "schema_change");
            schemaChange.put("ts", System.currentTimeMillis());
            
            // 尝试从Schema变更事件中提取数据库和表信息
            if (value.schema().field("database") != null) {
                schemaChange.put("db", value.getString("database"));
            }
            if (value.schema().field("table") != null) {
                schemaChange.put("tableName", value.getString("table"));
            }
            
            // 收集Schema变更事件
            collector.collect(schemaChange.toJSONString());
        } catch (Exception e) {
            log.error("处理Schema变更事件异常: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 将Struct转换为JSONObject
     */
    private JSONObject convertStruct(Struct struct) {
        JSONObject json = new JSONObject();
        if (struct == null) {
            return json;
        }
        
        try {
            Schema schema = struct.schema();
            List<Field> fields = schema.fields();
            
            for (Field field : fields) {
                String fieldName = field.name();
                Object value = struct.get(fieldName);
                
                // 处理复杂类型字段
                if (value instanceof Struct) {
                    json.put(fieldName, convertStruct((Struct) value));
                } 
                // 处理Map类型字段
                else if (value instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) value;
                    JSONObject mapJson = new JSONObject();
                    for (Map.Entry<String, Object> entry : map.entrySet()) {
                        mapJson.put(entry.getKey(), entry.getValue());
                    }
                    json.put(fieldName, mapJson);
                } 
                // 处理List类型字段
                else if (value instanceof List) {
                    json.put(fieldName, value); // 简单处理，直接放入
                } 
                // 处理时间戳类型字段，直接用UTC，不再加Asia/Shanghai，避免多加8小时
                else if (value instanceof Long && (fieldName.endsWith("_time") || fieldName.endsWith("_date") || fieldName.equals("create_time") || fieldName.equals("update_time"))) {
                    long timestamp = (Long) value;
                    java.time.LocalDateTime localDateTime = java.time.LocalDateTime.ofEpochSecond(timestamp / 1000, 0, java.time.ZoneOffset.UTC);
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    json.put(fieldName, localDateTime.format(formatter));
                }
                // 处理基本类型
                else {
                    json.put(fieldName, value);
                }
            }
        } catch (Exception e) {
            log.error("转换Struct到JSON异常: {}", e.getMessage(), e);
        }
        
        return json;
    }

    @Override
    public TypeInformation<String> getProducedType() {
        return BasicTypeInfo.STRING_TYPE_INFO;
    }
}
