package com.yxq.task.entity;

import lombok.Data;
import java.util.Date;

/**
 * 同步任务实体类
 * 对应数据表：y_task
 */
@Data
public class SyncTask {
    /**
     * 主键ID
     */
    private Integer id;
    
    /**
     * 任务名称
     */
    private String taskName;
    
    /**
     * 源数据库ID
     */
    private Integer sourceDbId;
    
    /**
     * 目标数据库ID
     */
    private Integer targetDbId;
    
    /**
     * 同步类型：0-全量同步，1-增量同步
     */
    private Integer syncType;
    
    /**
     * 同步表
     */
    private String tables;

    /**
     * 启动选项，JSON格式
     */
    private String startupOptions;
    
    /**
     * 任务状态：0-停止，1-运行中，2-异常
     */
    private Integer status;
    
    /**
     * 任务开始时间
     */
    private Date taskStartTime;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;
} 