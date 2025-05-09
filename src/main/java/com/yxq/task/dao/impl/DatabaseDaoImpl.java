package com.yxq.task.dao.impl;

import com.yxq.task.dao.DatabaseDao;
import com.yxq.task.entity.Database;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库DAO接口实现类
 */
@Slf4j
@Repository
public class DatabaseDaoImpl implements DatabaseDao {

    private final DataSource dataSource;

    public DatabaseDaoImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public int insert(Database database) {
        String sql = "INSERT INTO cdc_database_config (host, port, db_name, username, password, status, create_time, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, database.getHost());
            stmt.setInt(2, database.getPort());
            stmt.setString(3, database.getDbName());
            stmt.setString(4, database.getUsername());
            stmt.setString(5, database.getPassword());
            stmt.setInt(6, database.getStatus());
            stmt.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
            stmt.setTimestamp(8, new Timestamp(System.currentTimeMillis()));

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        database.setId(generatedKeys.getInt(1));
                    }
                }
            }

            return rows;
        } catch (SQLException e) {
            log.error("插入数据库连接信息失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public int update(Database database) {
        StringBuilder sql = new StringBuilder("UPDATE cdc_database_config SET update_time = ?");
        List<Object> params = new ArrayList<>();
        params.add(new Timestamp(System.currentTimeMillis()));

        if (database.getHost() != null) {
            sql.append(", host = ?");
            params.add(database.getHost());
        }
        if (database.getPort() != null) {
            sql.append(", port = ?");
            params.add(database.getPort());
        }
        if (database.getDbName() != null) {
            sql.append(", db_name = ?");
            params.add(database.getDbName());
        }
        if (database.getUsername() != null) {
            sql.append(", username = ?");
            params.add(database.getUsername());
        }
        if (database.getPassword() != null) {
            sql.append(", password = ?");
            params.add(database.getPassword());
        }
        if (database.getStatus() != null) {
            sql.append(", status = ?");
            params.add(database.getStatus());
        }
        if (database.getParam() != null) {
            sql.append(", param = ?");
            params.add(database.getParam());
        }
        sql.append(" WHERE id = ?");
        params.add(database.getId());

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            int index = 1;
            for (Object param : params) {
                stmt.setObject(index++, param);
            }
            log.info("更新数据库连接信息:{}", sql);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("更新数据库连接信息失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public int deleteById(Integer id) {
        String sql = "DELETE FROM cdc_database_config WHERE id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);

            return stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("删除数据库连接信息失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public Database selectById(Integer id) {
        String sql = "SELECT id, host, port, db_name, username, password, param, status, create_time, update_time FROM cdc_database_config WHERE id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDatabase(rs);
                }
            }
        } catch (SQLException e) {
            log.error("查询数据库连接信息失败: {}", e.getMessage(), e);
        }

        return null;
    }

    @Override
    public List<Database> selectAll() {
        String sql = "SELECT id, host, port, db_name, username, password, param, status, create_time, update_time FROM cdc_database_config";
        List<Database> databaseList = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                databaseList.add(mapResultSetToDatabase(rs));
            }
        } catch (SQLException e) {
            log.error("查询所有数据库连接信息失败: {}", e.getMessage(), e);
        }

        return databaseList;
    }

    @Override
    public int updateStatus(Integer id, Integer status) {
        String sql = "UPDATE cdc_database_config SET status = ? WHERE id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, status);
            stmt.setInt(2, id);

            return stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("更新数据库连接状态失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 将结果集映射为Database对象
     */
    private Database mapResultSetToDatabase(ResultSet rs) throws SQLException {
        Database database = new Database();
        database.setId(rs.getInt("id"));
        database.setHost(rs.getString("host"));
        database.setPort(rs.getInt("port"));
        database.setDbName(rs.getString("db_name"));
        database.setUsername(rs.getString("username"));
        database.setParam(rs.getString("param"));
        database.setPassword(rs.getString("password"));
        database.setStatus(rs.getInt("status"));
        database.setCreateTime(rs.getTimestamp("create_time"));
        database.setUpdateTime(rs.getTimestamp("update_time"));
        return database;
    }
} 