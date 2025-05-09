package com.yxq.task.util;

import com.yxq.task.entity.Database;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库连接和操作工具类
 */
@Slf4j
@Component
public class DatabaseUtil {

    private static DataSource dataSource;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        DatabaseUtil.dataSource = dataSource;
    }

    /**
     * 测试数据库连接
     *
     * @param database 数据库连接信息
     * @return 是否连接成功
     */
    public static boolean testConnection(Database database) {
        String url = buildJdbcUrl(database);
        try (Connection connection = DriverManager.getConnection(url, database.getUsername(), database.getPassword())) {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            log.error("数据库连接测试失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 构建JDBC URL
     *
     * @param database 数据库连接信息
     * @return JDBC URL
     */
    public static String buildJdbcUrl(Database database) {
        StringBuilder url = new StringBuilder("jdbc:mysql://")
                .append(database.getHost())
                .append(":")
                .append(database.getPort())
                .append("/")
                .append(database.getDbName())
                .append("?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai");
        
        // 添加额外参数
        if (database.getParam() != null && !database.getParam().isEmpty()) {
            url.append("&").append(database.getParam());
        }
        
        return url.toString();
    }
    
    /**
     * 获取指定数据库中的所有表名
     *
     * @param database 数据库连接信息
     * @return 表名列表
     */
    public static List<String> getAllTables(Database database) {
        String url = buildJdbcUrl(database);
        List<String> tables = new ArrayList<>();
        
        try (Connection connection = DriverManager.getConnection(url, database.getUsername(), AESUtil.decrypt(database.getPassword()))) {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet rs = metaData.getTables(database.getDbName(), null, "%", new String[]{"TABLE"});
            
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        } catch (SQLException e) {
            log.error("获取表名列表失败: {}", e.getMessage());
        }
        
        return tables;
    }
    
    /**
     * 获取表的列信息
     *
     * @param database 数据库连接信息
     * @param tableName 表名
     * @return 列信息列表，包含列名和数据类型
     */
    public static List<Map<String, String>> getTableColumns(Database database, String tableName) {
        String url = buildJdbcUrl(database);
        List<Map<String, String>> columns = new ArrayList<>();
        
        try (Connection connection = DriverManager.getConnection(url, database.getUsername(), AESUtil.decrypt(database.getPassword()))) {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet rs = metaData.getColumns(database.getDbName(), null, tableName, "%");
            
            while (rs.next()) {
                Map<String, String> column = new HashMap<>();
                column.put("name", rs.getString("COLUMN_NAME"));
                column.put("type", rs.getString("TYPE_NAME"));
                column.put("size", rs.getString("COLUMN_SIZE"));
                column.put("nullable", rs.getString("IS_NULLABLE"));
                columns.add(column);
            }
        } catch (SQLException e) {
            log.error("获取表列信息失败: {}", e.getMessage());
        }
        
        return columns;
    }
} 