package com.Inventory.DAO;

import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.Inventory.Bean.UserAccount;
import com.Inventory.DBUtil.DBUtil1;

@Repository
public class LoginDAO {

    private static final Pattern SAFE_TABLE_NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");

    private final String tableName;
    private final String accessTableName;
    private volatile JdbcTemplate jdbc;

    public LoginDAO(@Value("${app.auth.table:users}") String tableName,
                    @Value("${app.auth.access-table:user_database_access}") String accessTableName) {
        if (!SAFE_TABLE_NAME.matcher(tableName).matches()) {
            throw new IllegalArgumentException("app.auth.table must be a simple SQL table name.");
        }
        if (!SAFE_TABLE_NAME.matcher(accessTableName).matches()) {
            throw new IllegalArgumentException("app.auth.access-table must be a simple SQL table name.");
        }
        this.tableName = tableName;
        this.accessTableName = accessTableName;
    }

    public Optional<UserAccount> findByUsername(String username) {
        String sql = "SELECT id, username, password, role, department, mail, branch FROM `"
                + tableName + "` WHERE username = ? LIMIT 1";
        return jdbc().query(sql, rs -> {
            if (!rs.next()) return Optional.empty();
            UserAccount user = new UserAccount();
            user.setId(rs.getInt("id"));
            user.setUsername(rs.getString("username"));
            user.setPassword(rs.getString("password"));
            user.setRole(rs.getString("role"));
            user.setDepartment(rs.getString("department"));
            user.setMail(rs.getString("mail"));
            user.setBranch(rs.getString("branch"));
            return Optional.of(user);
        }, username);
    }

    /** Returns only configured company codes assigned to this user in the auth database. */
    public List<String> findAllowedDatabases(int userId) {
        String sql = "SELECT database_code FROM `" + accessTableName
                + "` WHERE user_id = ? ORDER BY database_code";
        List<String> allowed = new ArrayList<>();
        List<String> assigned = jdbc().query(sql,
                (rs, rowNum) -> rs.getString("database_code"), userId);
        for (String code : assigned) {
            // canonicalCompany is an application whitelist; ignore stale/invalid rows.
            try {
                String canonical = DBUtil1.canonicalCompany(code);
                if (!allowed.contains(canonical)) allowed.add(canonical);
            } catch (IllegalArgumentException ignored) { }
        }
        return allowed;
    }

    private JdbcTemplate jdbc() {
        JdbcTemplate current = jdbc;
        if (current == null) {
            synchronized (this) {
                current = jdbc;
                if (current == null) {
                    current = new JdbcTemplate(DBUtil1.getDataSource("SRS"));
                    jdbc = current;
                }
            }
        }
        return current;
    }
}
