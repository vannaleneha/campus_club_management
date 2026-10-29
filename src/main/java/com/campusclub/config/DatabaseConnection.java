package com.campusclub.config;

import com.campusclub.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Legacy database connection proxy.
 * Reconciled to delegate directly to com.campusclub.util.DBConnection
 * to preserve a single authoritative database connection and schema manager.
 */
public class DatabaseConnection {

    public static void setPassword(String pwd) {
        DBConnection.setPassword(pwd);
    }

    /**
     * Gets active database connection by delegating to DBConnection.
     */
    public static Connection getConnection() throws SQLException {
        return DBConnection.getConnection();
    }

    /**
     * Ensures the database and baseline tables exist by delegating to DBConnection.
     */
    public static void ensureDatabaseAndTableExist() throws SQLException {
        DBConnection.ensureDatabaseInitialized();
    }
}
