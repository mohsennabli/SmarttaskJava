package com.smarttask.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DatabaseConnection {
    private static final DbConfig CONFIG = resolveDbConfig();

    private static Connection connection;

    private DatabaseConnection() {
    }

    public static synchronized Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(CONFIG.url(), CONFIG.user(), CONFIG.password());
            }
            return connection;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC driver not found.", e);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database at " + CONFIG.url() + ".", e);
        }
    }

    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
                if (conn == connection) {
                    connection = null;
                }
            } catch (SQLException e) {
                System.err.println("Error while closing database connection: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        Connection conn = null;
        try {
            conn = getConnection();
            System.out.println("Connection successful!");
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
        } finally {
            closeConnection(conn);
        }
    }

    private static DbConfig resolveDbConfig() {
        String databaseUrl = EnvConfig.read("DATABASE_URL", "");
        if (!databaseUrl.isBlank()) {
            DbConfig parsed = parseSymfonyDatabaseUrl(databaseUrl);
            if (parsed != null) {
                return parsed;
            }
        }

        String host = EnvConfig.read("DB_HOST", "127.0.0.1");
        int port = EnvConfig.readInt("DB_PORT", 3306);
        String db = EnvConfig.read("DB_NAME", "smarttask");
        String user = EnvConfig.read("DB_USER", "root");
        String pass = EnvConfig.read("DB_PASSWORD", "");
        String jdbc = "jdbc:mysql://" + host + ":" + port + "/" + db;
        return new DbConfig(jdbc, user, pass);
    }

    private static DbConfig parseSymfonyDatabaseUrl(String raw) {
        String value = raw.trim();
        Pattern p = Pattern.compile("^mysql://([^:]+):([^@]*)@([^:/?#]+)(?::(\\d+))?/([^?]+).*$");
        Matcher m = p.matcher(value);
        if (!m.matches()) {
            return null;
        }
        String user = m.group(1);
        String pass = m.group(2);
        String host = m.group(3);
        String port = m.group(4) == null ? "3306" : m.group(4);
        String db = m.group(5);
        String jdbc = "jdbc:mysql://" + host + ":" + port + "/" + db;
        return new DbConfig(jdbc, user, pass);
    }

    private record DbConfig(String url, String user, String password) {
    }
}

