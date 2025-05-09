package com.yxq.task.service.impl;

import com.yxq.task.dao.DatabaseDao;
import com.yxq.task.entity.Database;
import com.yxq.task.service.DatabaseService;
import com.yxq.task.util.AESUtil;
import com.yxq.task.util.DatabaseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 数据库服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseServiceImpl implements DatabaseService {

    private final DatabaseDao databaseDao;

    @Override
    public boolean addDatabase(Database database) {
        // 先测试连接
        boolean isConnected = testConnection(database);
        if (!isConnected) {
            return false;
        }

        // 密码加密
        database.setPassword(AESUtil.encrypt(database.getPassword()));

        // 设置创建时间和更新时间
        Date now = new Date();
        database.setCreateTime(now);
        database.setUpdateTime(now);
        // 默认启用状态
        if (database.getStatus() == null) {
            database.setStatus(1);
        }

        return databaseDao.insert(database) > 0;
    }

    @Override
    public boolean updateDatabase(Database database) {
        // 获取原始数据
        Database oldDb = databaseDao.selectById(database.getId());
        if (oldDb == null) {
            return false;
        }

        // 如果密码有变更，则重新加密
        if (database.getPassword() != null && !database.getPassword().isEmpty()) {
            database.setPassword(AESUtil.encrypt(database.getPassword()));
        } else {
            // 使用原始密码
            database.setPassword(oldDb.getPassword());
        }

        // 更新时间
        database.setUpdateTime(new Date());

        return databaseDao.update(database) > 0;
    }

    @Override
    public boolean deleteDatabase(Integer id) {
        return databaseDao.deleteById(id) > 0;
    }

    @Override
    public Database getDatabaseById(Integer id) {
        return databaseDao.selectById(id);
    }

    @Override
    public List<Database> getAllDatabases() {
        return databaseDao.selectAll();
    }

    @Override
    public boolean updateStatus(Integer id, Integer status) {
        return databaseDao.updateStatus(id, status) > 0;
    }

    @Override
    public boolean testConnection(Database database) {
        // 如果密码已经加密，需要解密后再测试
        Database testDb = new Database();
        testDb.setHost(database.getHost());
        testDb.setPort(database.getPort());
        testDb.setDbName(database.getDbName());
        testDb.setUsername(database.getUsername());

        // 判断密码是否已加密
        String password = database.getPassword();
        if (password != null && password.length() % 4 == 0) {
            try {
                // 尝试Base64解码，如果成功则说明可能是加密的密码
                Base64.getDecoder().decode(password);
                // 尝试解密
                password = AESUtil.decrypt(password);
            } catch (Exception e) {
                // 解密失败，说明是未加密的密码
                password = database.getPassword();
            }
        } else {
            // 不是Base64编码，说明是未加密的密码
            password = database.getPassword();
        }

        testDb.setPassword(password);
        testDb.setParam(database.getParam());

        return DatabaseUtil.testConnection(testDb);
    }

    @Override
    public List<String> getAllTables(Integer databaseId) {
        Database database = databaseDao.selectById(databaseId);
        if (database == null) {
            return Collections.emptyList();
        }

        return DatabaseUtil.getAllTables(database);
    }

    @Override
    public List<Map<String, String>> getTableColumns(Integer databaseId, String tableName) {
        Database database = databaseDao.selectById(databaseId);
        if (database == null) {
            return Collections.emptyList();
        }

        return DatabaseUtil.getTableColumns(database, tableName);
    }
} 