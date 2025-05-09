package com.yxq.task.entity;

import lombok.Data;
import java.util.Date;

/**
 * 同步异常实体类
 */
@Data
public class SyncException {
    /**
     * 主键ID
     */
    private Integer id;
    
    /**
     * 任务ID
     */
    private Integer taskId;
    
    /**
     * 表名
     */
    private String tableName;
    
    /**
     * 错误消息
     */
    private String errorMessage;
    
    /**
     * 错误时间
     */
    private Date errorTime;
    
    /**
     * 处理时间
     */
    private Date handleTime;
    
    /**
     * 处理备注
     */
    private String handleRemark;
}