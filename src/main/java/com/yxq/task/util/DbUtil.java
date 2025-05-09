package com.yxq.task.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库操作工具类
 */
@Slf4j
@Component
public class DbUtil {

    private static DataSource dataSource;

    @Autowired
    public void setDataSource(DataSource dataSource) {
//        log.info("初始化DbUtil，设置数据源: {}", dataSource);
        DbUtil.dataSource = dataSource;
    }

    /**
     * 执行插入或更新SQL语句
     *
     * @param sql SQL语句
     * @return 影响行数
     */
    public static int insertOrUpdate(String sql) {
        if (dataSource == null) {
            log.error("数据源未初始化，无法执行SQL");
            return 0;
        }
        
        Connection connection = null;
        PreparedStatement stmt = null;
        
        try {
            log.debug("准备执行SQL: {}", sql);
            
            connection = dataSource.getConnection();
            if (connection == null) {
                log.error("无法获取数据库连接");
                return 0;
            }
            
            stmt = connection.prepareStatement(sql);
            int result = stmt.executeUpdate();
            
            log.debug("SQL执行完成，影响行数: {}", result);
            return result;
        } catch (SQLException e) {
            log.error("执行SQL失败: {}，错误: {}", sql, e.getMessage(), e);
            return 0;
        } finally {
            // 关闭资源
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log.error("关闭数据库资源失败", e);
            }
        }
    }

    /**
     * 执行插入或更新SQL语句（指定目标数据库）
     *
     * @param sql SQL语句
     * @param targetDbUrl 目标数据库URL
     * @param username 用户名
     * @param password 密码
     * @return 影响行数
     */
    public static int insertOrUpdateWithTargetDb(String sql, String targetDbUrl, String username, String password) {
        Connection connection = null;
        PreparedStatement stmt = null;
        
        try {
            log.debug("准备执行SQL到目标数据库[{}]: {}", targetDbUrl, sql);
            
            // 使用提供的连接信息创建新的连接
            connection = getConnection(targetDbUrl, username, password);
            if (connection == null) {
                log.error("无法获取目标数据库连接");
                return 0;
            }
            
            stmt = connection.prepareStatement(sql);
            int result = stmt.executeUpdate();
            
            log.debug("SQL执行完成，影响行数: {}", result);
            return result;
        } catch (SQLException e) {
            log.error("执行SQL到目标数据库失败: {}，错误: {}", sql, e.getMessage(), e);
            return 0;
        } finally {
            // 关闭资源
            closeConnection(stmt, connection);
        }
    }

    /**
     * 获取数据库连接
     *
     * @param url 数据库URL
     * @param username 用户名
     * @param password 密码
     * @return 数据库连接
     * @throws SQLException SQL异常
     */
    public static Connection getConnection(String url, String username, String password) throws SQLException {
        try {
            log.debug("获取数据库连接: {}, 用户名: {}", url, username);
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            log.error("获取数据库连接失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 关闭数据库连接和资源
     *
     * @param stmt PreparedStatement
     * @param connection 数据库连接
     */
    public static void closeConnection(PreparedStatement stmt, Connection connection) {
        try {
            if (stmt != null) {
                stmt.close();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            log.error("关闭数据库资源失败", e);
        }
    }

    /**
     * 执行插入或更新SQL语句（使用外部连接）
     *
     * @param connection 数据库连接
     * @param sql SQL语句
     * @return 影响行数
     */
    public static int insertOrUpdate(Connection connection, String sql) {
        if (connection == null) {
            log.error("数据库连接为null，无法执行SQL");
            return 0;
        }
        
        PreparedStatement stmt = null;
        
        try {
            log.debug("准备使用外部连接执行SQL: {}", sql);
            
            stmt = connection.prepareStatement(sql);
            int result = stmt.executeUpdate();
            
            log.debug("SQL执行完成，影响行数: {}", result);
            return result;
        } catch (SQLException e) {
            log.error("执行SQL失败: {}，错误: {}", sql, e.getMessage(), e);
            return 0;
        } finally {
            // 关闭资源，但不关闭外部传入的连接
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
                log.error("关闭PreparedStatement失败", e);
            }
        }
    }
    
    /**
     * 测试数据源连接是否正常
     * 
     * @return 连接是否正常
     */
    public static boolean testDataSource() {
        if (dataSource == null) {
            log.error("数据源未初始化，无法测试连接");
            return false;
        }
        
        Connection connection = null;
        try {
            log.info("测试数据源连接");
            connection = dataSource.getConnection();
            
            boolean isValid = connection != null && !connection.isClosed();
            log.info("数据源连接测试结果: {}", isValid ? "正常" : "异常");
            
            return isValid;
        } catch (SQLException e) {
            log.error("测试数据源连接失败: {}", e.getMessage(), e);
            return false;
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log.error("关闭测试连接失败", e);
            }
        }
    }

    /**
     * 执行查询SQL语句并返回结果集
     *
     * @param sql SQL查询语句
     * @return 结果列表（每行记录是一个Map）
     */
    public static List<Map<String, Object>> executeQuery(String sql) {
        if (dataSource == null) {
            log.error("数据源未初始化，无法执行SQL");
            return Collections.emptyList();
        }
        
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            log.debug("准备执行查询SQL: {}", sql);
            
            connection = dataSource.getConnection();
            if (connection == null) {
                log.error("无法获取数据库连接");
                return Collections.emptyList();
            }
            
            stmt = connection.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            List<Map<String, Object>> resultList = new ArrayList<>();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                resultList.add(row);
            }
            
            log.debug("查询执行完成，返回 {} 条记录", resultList.size());
            return resultList;
        } catch (SQLException e) {
            log.error("执行查询SQL失败: {}，错误: {}", sql, e.getMessage(), e);
            return Collections.emptyList();
        } finally {
            // 关闭资源
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log.error("关闭数据库资源失败", e);
            }
        }
    }
} 