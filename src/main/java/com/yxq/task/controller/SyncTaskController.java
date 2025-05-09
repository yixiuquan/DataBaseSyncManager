package com.yxq.task.controller;

import com.yxq.task.entity.SyncTask;
import com.yxq.task.service.DatabaseService;
import com.yxq.task.service.SyncTaskService;
import com.yxq.task.util.ResultVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 同步任务控制器
 */
@RestController
@RequestMapping("/db")
@RequiredArgsConstructor
@Api(tags = "同步任务管理控制器")
public class SyncTaskController {
    @Autowired
    private SyncTaskService syncTaskService;
    @Autowired
    private DatabaseService databaseService;
    
    /**
     * 获取所有同步任务
     *
     * @return 同步任务列表
     */
    @GetMapping("getAllSyncTasks")
    @ApiOperation("获取所有同步任务")
    public ResultVO<List<SyncTask>> getAllSyncTasks() {
        List<SyncTask> tasks = syncTaskService.getAllSyncTasks();
        return ResultVO.success(tasks);
    }
    
    /**
     * 根据ID获取同步任务
     *
     * @param id 任务ID
     * @return 同步任务
     */
    @GetMapping("getSyncTaskById")
    @ApiOperation("根据ID获取同步任务")
    @ApiImplicitParam(name = "id", value = "任务ID", required = true, dataType = "Integer", example = "1")
    public ResultVO<SyncTask> getSyncTaskById(@RequestParam("id") Integer id) {
        SyncTask task = syncTaskService.getSyncTaskById(id);
        if (task == null) {
            return ResultVO.error("同步任务不存在");
        }
        return ResultVO.success(task);
    }
    
    /**
     * 添加同步任务
     *
     * @param syncTask 同步任务
     * @return 操作结果
     */
    @PostMapping("addSyncTask")
    @ApiOperation("添加同步任务")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "taskName", value = "任务名称", required = true, dataType = "String", example = "数据同步任务1"),
            @ApiImplicitParam(name = "sourceDbId", value = "源数据库ID", required = true, dataType = "Integer", example = "1"),
            @ApiImplicitParam(name = "targetDbId", value = "目标数据库ID", required = true, dataType = "Integer", example = "2"),
            @ApiImplicitParam(name = "syncType", value = "同步类型", required = true, dataType = "Integer", example = "1"),
            @ApiImplicitParam(name = "tables", value = "同步表配置", required = true, dataType = "String", example = "user_table,order_table")
    })
    public ResultVO<Boolean> addSyncTask(@RequestBody SyncTask syncTask) {
        // 验证必要字段
        if (syncTask.getTaskName() == null || syncTask.getTaskName().isEmpty()) {
            return ResultVO.error("任务名称不能为空");
        }
        if (syncTask.getSourceDbId() == null) {
            return ResultVO.error("源数据库ID不能为空");
        }
        if (syncTask.getTargetDbId() == null) {
            return ResultVO.error("目标数据库ID不能为空");
        }
        if (syncTask.getSyncType() == null) {
            return ResultVO.error("同步类型不能为空");
        }
        if (syncTask.getTables() == null || syncTask.getTables().isEmpty()) {
            return ResultVO.error("同步表配置不能为空");
        }
        
        // 验证数据库是否存在
        if (databaseService.getDatabaseById(syncTask.getSourceDbId()) == null) {
            return ResultVO.error("源数据库不存在");
        }
        if (databaseService.getDatabaseById(syncTask.getTargetDbId()) == null) {
            return ResultVO.error("目标数据库不存在");
        }
        
        boolean result = syncTaskService.addSyncTask(syncTask);
        if (result) {
            return ResultVO.success(true);
        } else {
            return ResultVO.error("添加同步任务失败");
        }
    }
    
    /**
     * 更新同步任务
     *
     * @param syncTask 同步任务
     * @return 操作结果
     */
    @PostMapping("updateSyncTask")
    @ApiOperation("更新同步任务")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "任务ID", required = true, dataType = "Integer", example = "1"),
            @ApiImplicitParam(name = "taskName", value = "任务名称", dataType = "String", example = "数据同步任务1"),
            @ApiImplicitParam(name = "sourceDbId", value = "源数据库ID", dataType = "Integer", example = "1"),
            @ApiImplicitParam(name = "targetDbId", value = "目标数据库ID", dataType = "Integer", example = "2"),
            @ApiImplicitParam(name = "syncType", value = "同步类型", dataType = "Integer", example = "1"),
            @ApiImplicitParam(name = "tables", value = "同步表配置", dataType = "String", example = "user_table,order_table")
    })
    public ResultVO<Boolean> updateSyncTask(@RequestBody SyncTask syncTask) {
        if (syncTask.getId() == null) {
            return ResultVO.error("任务ID不能为空");
        }
        
        SyncTask existTask = syncTaskService.getSyncTaskById(syncTask.getId());
        if (existTask == null) {
            return ResultVO.error("同步任务不存在");
        }
        
        // 如果任务正在运行，不允许修改
        if (existTask.getStatus() == 1) {
            return ResultVO.error("任务正在运行，无法修改");
        }
        
        boolean result = syncTaskService.updateSyncTask(syncTask);
        if (result) {
            return ResultVO.success(true);
        } else {
            return ResultVO.error("更新同步任务失败");
        }
    }
    
    /**
     * 删除同步任务
     *
     * @param id 任务ID
     * @return 操作结果
     */
    @PostMapping("deleteSyncTask")
    @ApiOperation("删除同步任务")
    @ApiImplicitParam(name = "id", value = "任务ID", required = true, dataType = "Integer", example = "1")
    public ResultVO<Boolean> deleteSyncTask(@RequestParam("id") Integer id) {
        SyncTask existTask = syncTaskService.getSyncTaskById(id);
        if (existTask == null) {
            return ResultVO.error("同步任务不存在");
        }
        
        // 如果任务正在运行，不允许删除
        if (existTask.getStatus() == 1) {
            return ResultVO.error("任务正在运行，无法删除");
        }
        
        boolean result = syncTaskService.deleteSyncTask(id);
        if (result) {
            return ResultVO.success(true);
        } else {
            return ResultVO.error("删除同步任务失败");
        }
    }
    
    /**
     * 启动同步任务
     *
     * @param id 任务ID
     * @return 操作结果
     */
    @PostMapping("startTask")
    @ApiOperation("启动同步任务")
    @ApiImplicitParam(name = "id", value = "任务ID", required = true, dataType = "Integer", example = "1")
    public ResultVO<Boolean> startTask(@RequestParam("id") Integer id) {
        SyncTask existTask = syncTaskService.getSyncTaskById(id);
        if (existTask == null) {
            return ResultVO.error("同步任务不存在");
        }
        
        // 如果任务已经在运行，直接返回成功
        if (existTask.getStatus() == 1) {
            return ResultVO.success(true);
        }
        
        boolean result = syncTaskService.startTask(id);
        if (result) {
            return ResultVO.success(true);
        } else {
            return ResultVO.error("启动同步任务失败");
        }
    }
    
    /**
     * 停止同步任务
     *
     * @param id 任务ID
     * @return 操作结果
     */
    @PostMapping("stopTask")
    @ApiOperation("停止同步任务")
    @ApiImplicitParam(name = "id", value = "任务ID", required = true, dataType = "Integer", example = "1")
    public ResultVO<Boolean> stopTask(@RequestParam("id") Integer id) {
        SyncTask existTask = syncTaskService.getSyncTaskById(id);
        if (existTask == null) {
            return ResultVO.error("同步任务不存在");
        }
        
        // 如果任务未运行，直接返回成功
        if (existTask.getStatus() != 1) {
            return ResultVO.success(true);
        }
        
        boolean result = syncTaskService.stopTask(id);
        if (result) {
            return ResultVO.success(true);
        } else {
            return ResultVO.error("停止同步任务失败");
        }
    }
    
    /**
     * 获取任务监控统计信息
     *
     * @param id 任务ID
     * @return 监控统计信息
     */
    @GetMapping("getTaskStatistics")
    @ApiOperation("获取任务监控统计信息")
    @ApiImplicitParam(name = "id", value = "任务ID", required = true, dataType = "Integer", example = "1")
    public ResultVO<Map<String, Object>> getTaskStatistics(@RequestParam("id") Integer id) {
        SyncTask existTask = syncTaskService.getSyncTaskById(id);
        if (existTask == null) {
            return ResultVO.error("同步任务不存在");
        }
        
        Map<String, Object> statistics = syncTaskService.getTaskStatistics(id);
        if (statistics == null) {
            return ResultVO.error("获取任务监控统计信息失败");
        }
        
        return ResultVO.success(statistics);
    }
} 