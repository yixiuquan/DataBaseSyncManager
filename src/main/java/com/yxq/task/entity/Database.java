package com.yxq.task.entity;

import lombok.Data;
import java.util.Date;

/**
 * 数据库连接信息实体类
 * 对应数据表：y_database
 */
@Data
public class Database {
    /**
     * 主键ID
     */
    private Integer id;
    
    /**
     * 主机地址
     */
    private String host;
    
    /**
     * 端口
     */
    private Integer port;
    
    /**
     * 数据库名称
     */
    private String dbName;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码（加密存储）
     */
    private String password;
    
    /**
     * 额外连接参数
     */
    private String param;
    
    /**
     * 状态：0-停用，1-启用
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;
} 