package com.smarttask.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBconnexion {
    public String url = "jdbc:mysql://localhost:3306/db";
    public String login = "root";
    public String pwd = "";

    private Connection cnx;
    public static DBconnexion instance;

    private DBconnexion() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            cnx = DriverManager.getConnection(url, login, pwd);
            System.out.println("Connection etablie");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MySQL JDBC driver not found in classpath", e);
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public Connection getCnx() {
        try {
            if (cnx == null || cnx.isClosed()) {
                cnx = DriverManager.getConnection(url, login, pwd);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to get database connection", e);
        }
        return cnx;
    }

    public static DBconnexion getInstance() {
        if (instance == null) {
            instance = new DBconnexion();
        }
        return instance;
    }
}