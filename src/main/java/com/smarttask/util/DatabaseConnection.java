package com.smarttask.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {
    private static final String URL = "jdbc:mysql://127.0.0.1:3306/smarttask";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static Connection connection;

    private DatabaseConnection() {
    }

    public static synchronized Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            }
            return connection;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC driver not found.", e);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database at " + URL + ".", e);
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
}

