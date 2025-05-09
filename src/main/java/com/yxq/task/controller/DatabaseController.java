package com.yxq.task.controller;

import com.yxq.task.entity.Database;
import com.yxq.task.service.DatabaseService;
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
 * 数据库管理控制器
 */
@RestController
@RequestMapping("/db")
@RequiredArgsConstructor
@Api(tags = "数据库管理控制器")
public class DatabaseController {

    @Autowired
    private DatabaseService databaseService;

    /**
     * 根据ID获取数据库连接信息
     *
     * @param id 数据库ID
     * @return 数据库连接信息
     */
    @GetMapping("getDatabaseById")
    @ApiOperation("根据ID获取数据库连接信息")
    @ApiImplicitParam(name = "id", value = "数据库ID", required = false, dataType = "Integer", example = "1")
    public ResultVO<Database> getDatabaseById(@RequestParam("id") Integer id) {
        Database database = databaseService.getDatabaseById(id);
        if (database == null) {
            return ResultVO.error("数据库连接信息不存在");
        }
        return ResultVO.success(database);
    }
    /**
     * 获取所有数据库连接信息
     * @return 数据库连接信息
     */
    @GetMapping("getAllDatabases")
    @ApiOperation("根据ID获取数据库连接信息")
    public ResultVO<List<Database>> getAllDatabases() {
        List<Database> allDatabases = databaseService.getAllDatabases();
        if (allDatabases == null) {
            return ResultVO.error("数据库连接信息不存在");
        }
        return ResultVO.success(allDatabases);
    }

    /**
     * 添加数据库连接信息
     *
     * @param database 数据库连接信息
     * @return 操作结果
     */
    @PostMapping("addDatabase")
    @ApiOperation("添加数据库连接信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "host", value = "主机地址", required = true, dataType = "String", example = "localhost"),
            @ApiImplicitParam(name = "port", value = "端口", required = true, dataType = "String", example = "3306"),
            @ApiImplicitParam(name = "dbName", value = "数据库名称", required = false, dataType = "String", example = "mydatabase"),
            @ApiImplicitParam(name = "username", value = "用户名", required = true, dataType = "String", example = "root"),
            @ApiImplicitParam(name = "password", value = "密码", required = true, dataType = "String", example = "123456")
    })
    public ResultVO<Boolean> addDatabase(@RequestBody Database database) {
        if (database.getHost() == null || database.getHost().isEmpty()) {
            return ResultVO.error("主机地址不能为空");
        }
        if (database.getPort() == null) {
            return ResultVO.error("端口不能为空");
        }
        if (database.getUsername() == null || database.getUsername().isEmpty()) {
            return ResultVO.error("用户名不能为空");
        }
        if (database.getPassword() == null || database.getPassword().isEmpty()) {
            return ResultVO.error("密码不能为空");
        }

        boolean isConnected = databaseService.testConnection(database);
        if (!isConnected) {
            return ResultVO.error("数据库连接测试失败，请检查连接信息");
        }

        boolean result = databaseService.addDatabase(database);
        if (result) {
            return ResultVO.success(true);
        } else {
            return ResultVO.error("添加数据库连接信息失败");
        }
    }

    /**
     * 更新数据库连接信息
     *
     * @param database 数据库连接信息
     * @return 操作结果
     */
    @PostMapping("updateDatabase")
    @ApiOperation("更新数据库连接信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "数据库ID", required = true, dataType = "Integer", example = "1"),
            @ApiImplicitParam(name = "host", value = "主机地址", dataType = "String", example = "localhost"),
            @ApiImplicitParam(name = "port", value = "端口", dataType = "Integer", example = "3306"),
            @ApiImplicitParam(name = "dbName", value = "数据库名称", dataType = "String", example = "mydatabase"),
            @ApiImplicitParam(name = "username", value = "用户名", dataType = "String", example = "root"),
            @ApiImplicitParam(name = "password", value = "密码", dataType = "String", example = "123456"),
            @ApiImplicitParam(name = "status", value = "状态", dataType = "Integer", example = "1"),
            @ApiImplicitParam(name = "param", value = "参数", dataType = "String", example = "{\"key\":\"value\"}")
    })
    public ResultVO<Boolean> updateDatabase(@RequestBody Database database) {
        if (database.getId() == null) {
            return ResultVO.error("数据库ID不能为空");
        }

        Database existDatabase = databaseService.getDatabaseById(database.getId());
        if (existDatabase == null) {
            return ResultVO.error("数据库连接信息不存在");
        }

        boolean result = databaseService.updateDatabase(database);
        if (result) {
            return ResultVO.success(true);
        } else {
            return ResultVO.error("更新数据库连接信息失败");
        }
    }

    /**
     * 删除数据库连接信息
     *
     * @param id 数据库ID
     * @return 操作结果
     */
    @PostMapping("deleteDatabase")
    @ApiOperation("删除数据库连接信息")
    @ApiImplicitParam(name = "id", value = "数据库ID", required = true, dataType = "Integer", example = "1")
    public ResultVO<Boolean> deleteDatabase(@RequestParam("id") Integer id) {
        Database existDatabase = databaseService.getDatabaseById(id);
        if (existDatabase == null) {
            return ResultVO.error("数据库连接信息不存在");
        }

        boolean result = databaseService.deleteDatabase(id);
        if (result) {
            return ResultVO.success(true);
        } else {
            return ResultVO.error("删除数据库连接信息失败");
        }
    }

    /**
     * 修改数据库连接状态
     *
     * @param id 数据库ID
     * @param status 状态（0=停用 1=启用）
     * @return 操作结果
     */
    @PostMapping("updateStatus")
    @ApiOperation("修改数据库连接状态")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "数据库ID", required = true, dataType = "Integer", example = "1"),
            @ApiImplicitParam(name = "status", value = "状态（0=停用 1=启用）", required = true, dataType = "Integer", example = "1")
    })
    public ResultVO<Boolean> updateStatus(@RequestParam("id") Integer id, @RequestParam("status") Integer status) {
        Database existDatabase = databaseService.getDatabaseById(id);
        if (existDatabase == null) {
            return ResultVO.error("数据库连接信息不存在");
        }

        if (status != 0 && status != 1) {
            return ResultVO.error("状态值无效");
        }

        boolean result = databaseService.updateStatus(id, status);
        if (result) {
            return ResultVO.success(true);
        } else {
            return ResultVO.error("修改数据库连接状态失败");
        }
    }

    /**
     * 测试数据库连接
     *
     * @param database 数据库连接信息
     * @return 操作结果
     */
    @PostMapping("testConnection")
    @ApiOperation("测试数据库连接")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "host", value = "主机地址", required = true, dataType = "String", example = "localhost"),
            @ApiImplicitParam(name = "port", value = "端口", required = true, dataType = "Integer", example = "3306"),
            @ApiImplicitParam(name = "dbName", value = "数据库名称", required = false, dataType = "String", example = "mydatabase"),
            @ApiImplicitParam(name = "username", value = "用户名", required = true, dataType = "String", example = "root"),
            @ApiImplicitParam(name = "password", value = "密码", required = true, dataType = "String", example = "123456")
    })
    public ResultVO<Boolean> testConnection(@RequestBody Database database) {
        if (database.getId() == null) {
            if (database.getHost() == null || database.getHost().isEmpty()) {
                return ResultVO.error("主机地址不能为空");
            }
            if (database.getPort() == null) {
                return ResultVO.error("端口不能为空");
            }
            if (database.getUsername() == null || database.getUsername().isEmpty()) {
                return ResultVO.error("用户名不能为空");
            }
            if (database.getPassword() == null || database.getPassword().isEmpty()) {
                return ResultVO.error("密码不能为空");
            }
        }else {
            database = databaseService.getDatabaseById(database.getId());
        }

        boolean isConnected = databaseService.testConnection(database);
        if (isConnected) {
            return ResultVO.success(true);
        } else {
            return ResultVO.error("数据库连接测试失败，请检查连接信息");
        }
    }

    /**
     * 获取数据库中所有表
     *
     * @param databaseId 数据库ID
     * @return 表名列表
     */
    @GetMapping("getAllTables")
    @ApiOperation("获取数据库中所有表")
    @ApiImplicitParam(name = "databaseId", value = "数据库ID", required = true, dataType = "Integer", example = "1")
    public ResultVO<List<String>> getAllTables(@RequestParam("databaseId") Integer databaseId) {
        Database existDatabase = databaseService.getDatabaseById(databaseId);
        if (existDatabase == null) {
            return ResultVO.error("数据库连接信息不存在");
        }

        if (existDatabase.getStatus() == 0) {
            return ResultVO.error("数据库连接已停用");
        }

        List<String> tables = databaseService.getAllTables(databaseId);
        return ResultVO.success(tables);
    }

    /**
     * 获取表的列信息
     *
     * @param databaseId 数据库ID
     * @param tableName 表名
     * @return 列信息列表
     */
    @GetMapping("getTableColumns")
    @ApiOperation("获取表的列信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "databaseId", value = "数据库ID", required = true, dataType = "Integer", example = "1"),
            @ApiImplicitParam(name = "tableName", value = "表名", required = true, dataType = "String", example = "user_table")
    })
    public ResultVO<List<Map<String, String>>> getTableColumns(@RequestParam("databaseId") Integer databaseId, @RequestParam("tableName") String tableName) {
        Database existDatabase = databaseService.getDatabaseById(databaseId);
        if (existDatabase == null) {
            return ResultVO.error("数据库连接信息不存在");
        }

        if (existDatabase.getStatus() == 0) {
            return ResultVO.error("数据库连接已停用");
        }

        List<Map<String, String>> columns = databaseService.getTableColumns(databaseId, tableName);
        return ResultVO.success(columns);
    }
} 