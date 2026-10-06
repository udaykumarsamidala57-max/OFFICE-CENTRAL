package com.Inventory.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.AbstractDataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/** Creates one pooled connection source for each allowed company database. */
public final class DBUtil1 {

    private static final String HOST = setting("DB_HOST", "");
    private static final String PORT = setting("DB_PORT", "");
    private static final String USER = setting("DB_USER", "");
    private static final String PASSWORD = setting("DB_PASSWORD", "");

    private static final Map<String, String> DATABASES = Map.of(
            "SRS", "inventory",
            "Sanpoly", "SANPOLY_INVENTORY",
            "Sanpoly2", "SANPOLY_INVENTORY2");

    private static final Map<String, HikariDataSource> POOLS = new ConcurrentHashMap<>();
    private static final Map<String, DataSource> DATA_SOURCES = new ConcurrentHashMap<>();

    private DBUtil1() { }

    public static Connection getConnection(String company) throws SQLException {
        return getDataSource(company).getConnection();
    }

    public static DataSource getDataSource(String company) {
        String canonical = canonicalCompany(company);
        return DATA_SOURCES.computeIfAbsent(canonical, DBUtil1::createLazyDataSource);
    }

    public static String canonicalCompany(String company) {
        if (company == null) {
            throw new IllegalArgumentException("Company selection is required.");
        }
        return DATABASES.keySet().stream()
                .filter(name -> name.equalsIgnoreCase(company.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported company selection."));
    }

    private static DataSource createLazyDataSource(String company) {
        return new AbstractDataSource() {
            private HikariDataSource pool() {
                return POOLS.computeIfAbsent(company, DBUtil1::createDataSource);
            }

            @Override
            public Connection getConnection() throws SQLException {
                return pool().getConnection();
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                return pool().getConnection(username, password);
            }
        };
    }
    private static HikariDataSource createDataSource(String company) {
        if (HOST.isBlank() || PORT.isBlank() || USER.isBlank() || PASSWORD.isBlank()) {
            throw new IllegalStateException("Set DB_HOST, DB_PORT, DB_USER, and DB_PASSWORD environment variables before connecting to a company database.");
        }

        String database = DATABASES.get(company);
        String url = "jdbc:mysql://" + HOST + ":" + PORT + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&tcpKeepAlive=true";

        HikariConfig config = new HikariConfig();
        config.setPoolName("erp-" + company);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl(url);
        config.setUsername(USER);
        config.setPassword(PASSWORD);
        config.setMaximumPoolSize(8);
        config.setMinimumIdle(0);
        config.setConnectionTimeout(15000);
        return new HikariDataSource(config);
    }

    private static String setting(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) value = System.getProperty(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}



