package com.yxq.task.controller;

import com.yxq.task.entity.SyncException;
import com.yxq.task.service.SyncExceptionService;
import com.yxq.task.util.ResultVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 同步异常控制器
 */
@RestController
@RequestMapping("/exception")
@RequiredArgsConstructor
@Api(tags = "同步异常管理控制器")
public class SyncExceptionController {
    @Autowired
    private SyncExceptionService syncExceptionService;
    
    /**
     * 获取指定任务的所有异常记录
     *
     * @param taskId 任务ID
     * @return 异常记录列表
     */
    @GetMapping("getByTaskId")
    @ApiOperation("获取任务异常记录")
    @ApiImplicitParam(name = "taskId", value = "任务ID", required = true, dataType = "Integer", example = "1")
    public ResultVO<List<SyncException>> getByTaskId(@RequestParam("taskId") Integer taskId) {
        List<SyncException> exceptions = syncExceptionService.getByTaskId(taskId);
        return ResultVO.success(exceptions);
    }
    
    /**
     * 获取指定任务和表的异常记录
     *
     * @param taskId 任务ID
     * @param tableName 表名
     * @return 异常记录列表
     */
    @GetMapping("getByTaskIdAndTableName")
    @ApiOperation("获取任务指定表的异常记录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "taskId", value = "任务ID", required = true, dataType = "Integer", example = "1"),
            @ApiImplicitParam(name = "tableName", value = "表名", required = true, dataType = "String", example = "user_table")
    })
    public ResultVO<List<SyncException>> getByTaskIdAndTableName(
            @RequestParam("taskId") Integer taskId,
            @RequestParam("tableName") String tableName) {
        List<SyncException> exceptions = syncExceptionService.getByTaskIdAndTableName(taskId, tableName);
        return ResultVO.success(exceptions);
    }
    
    /**
     * 获取异常记录计数
     *
     * @param taskId 任务ID
     * @return 异常记录数
     */
    @GetMapping("countByTaskId")
    @ApiOperation("获取任务异常记录数")
    @ApiImplicitParam(name = "taskId", value = "任务ID", required = true, dataType = "Integer", example = "1")
    public ResultVO<Integer> countByTaskId(@RequestParam("taskId") Integer taskId) {
        int count = syncExceptionService.countByTaskId(taskId);
        return ResultVO.success(count);
    }
    
    /**
     * 获取指定任务和表的异常记录计数
     *
     * @param taskId 任务ID
     * @param tableName 表名
     * @return 异常记录数
     */
    @GetMapping("countByTaskIdAndTableName")
    @ApiOperation("获取任务指定表的异常记录数")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "taskId", value = "任务ID", required = true, dataType = "Integer", example = "1"),
            @ApiImplicitParam(name = "tableName", value = "表名", required = true, dataType = "String", example = "user_table")
    })
    public ResultVO<Integer> countByTaskIdAndTableName(
            @RequestParam("taskId") Integer taskId,
            @RequestParam("tableName") String tableName) {
        int count = syncExceptionService.countByTaskIdAndTableName(taskId, tableName);
        return ResultVO.success(count);
    }
} 