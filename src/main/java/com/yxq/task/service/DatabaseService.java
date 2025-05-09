package com.yxq.task.service;

import com.yxq.task.entity.Database;
import java.util.List;
import java.util.Map;

/**
 * 数据库服务接口
 */
public interface DatabaseService {
    
    /**
     * 添加数据库连接信息
     *
     * @param database 数据库连接信息
     * @return 添加是否成功
     */
    boolean addDatabase(Database database);
    
    /**
     * 更新数据库连接信息
     *
     * @param database 数据库连接信息
     * @return 更新是否成功
     */
    boolean updateDatabase(Database database);
    
    /**
     * 删除数据库连接信息
     *
     * @param id 数据库ID
     * @return 删除是否成功
     */
    boolean deleteDatabase(Integer id);
    
    /**
     * 根据ID获取数据库连接信息
     *
     * @param id 数据库ID
     * @return 数据库连接信息
     */
    Database getDatabaseById(Integer id);
    
    /**
     * 获取所有数据库连接信息列表
     *
     * @return 数据库连接信息列表
     */
    List<Database> getAllDatabases();
    
    /**
     * 修改数据库连接状态
     *
     * @param id 数据库ID
     * @param status 状态（0=停用 1=启用）
     * @return 修改是否成功
     */
    boolean updateStatus(Integer id, Integer status);
    
    /**
     * 测试数据库连接
     *
     * @param database 数据库连接信息
     * @return 是否连接成功
     */
    boolean testConnection(Database database);
    
    /**
     * 获取数据库中所有表
     *
     * @param databaseId 数据库ID
     * @return 表名列表
     */
    List<String> getAllTables(Integer databaseId);
    
    /**
     * 获取表的列信息
     *
     * @param databaseId 数据库ID
     * @param tableName 表名
     * @return 列信息列表
     */
    List<Map<String, String>> getTableColumns(Integer databaseId, String tableName);
} 