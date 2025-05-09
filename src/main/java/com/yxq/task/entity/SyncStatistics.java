package com.yxq.task.entity;

import lombok.Data;
import java.util.Date;

/**
 * 同步统计信息实体类
 */
@Data
public class SyncStatistics {
    /**
     * 主键ID
     */
    private Integer id;
    
    /**
     * 任务ID
     */
    private Integer taskId;
    
    /**
     * 执行记录ID
     */
    private Integer executionId;
    /**
     * 同步类型
     */
    
    /**
     * 表名
     */
    private String tableName;
    
    /**
     * 已同步数据量
     */
    private Integer syncCount;
    
    /**
     * 总数据量
     */
    private Integer totalCount;
    
    /**
     * 异常记录数
     */
    private Integer exceptionCount;
    
    /**
     * 表同步开始时间
     */
    private Date startTime;
    
    /**
     * 最近更新时间
     */
    private Date lastUpdateTime;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;

    private Double progress;
    
    /**
     * 插入记录数
     */
    private Integer insertCount;
    
    /**
     * 更新记录数
     */
    private Integer updateCount;
    
    /**
     * 删除记录数
     */
    private Integer deleteCount;
    
    /**
     * 额外信息JSON
     */
    private String remark;
}