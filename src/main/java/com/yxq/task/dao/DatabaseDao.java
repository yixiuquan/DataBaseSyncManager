package com.yxq.task.dao;

import com.yxq.task.entity.Database;
import java.util.List;

/**
 * 数据库DAO接口
 */
public interface DatabaseDao {
    
    /**
     * 插入数据库连接信息
     *
     * @param database 数据库连接信息
     * @return 影响行数
     */
    int insert(Database database);
    
    /**
     * 更新数据库连接信息
     *
     * @param database 数据库连接信息
     * @return 影响行数
     */
    int update(Database database);
    
    /**
     * 根据ID删除数据库连接信息
     *
     * @param id 数据库ID
     * @return 影响行数
     */
    int deleteById(Integer id);
    
    /**
     * 根据ID查询数据库连接信息
     *
     * @param id 数据库ID
     * @return 数据库连接信息
     */
    Database selectById(Integer id);
    
    /**
     * 查询所有数据库连接信息
     *
     * @return 数据库连接信息列表
     */
    List<Database> selectAll();
    
    /**
     * 更新数据库连接状态
     *
     * @param id 数据库ID
     * @param status 状态（0=停用 1=启用）
     * @return 影响行数
     */
    int updateStatus(Integer id, Integer status);
} 