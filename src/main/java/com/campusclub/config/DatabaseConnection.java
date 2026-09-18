package com.campusclub.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Singleton database connection manager for MySQL backend.
 * Database: campus_club
 * Host: localhost:3306
 * Username: root
 */
public class DatabaseConnection {

    private static final String HOST = "localhost";
    private static final int PORT = 3306;
    private static final String DB_NAME = "campus_club";
    private static final String USER = "root";

    // Read password from system property or environment variable, fallback to empty string
    private static String password = System.getProperty("db.password", System.getenv().getOrDefault("DB_PASSWORD", ""));

    private static Connection connection = null;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found: " + e.getMessage());
        }
    }

    public static void setPassword(String pwd) {
        password = pwd;
        connection = null; // Reset connection if password changes
    }

    public static String getPassword() {
        return password;
    }

    /**
     * Gets active database connection to campus_club.
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            ensureDatabaseAndTableExist();
            String url = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            connection = DriverManager.getConnection(url, USER, password);
        }
        return connection;
    }

    /**
     * Ensures the 'campus_club' database, 'users' table, and seed data exist.
     */
    public static void ensureDatabaseAndTableExist() throws SQLException {
        String serverUrl = "jdbc:mysql://" + HOST + ":" + PORT + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        try (Connection conn = DriverManager.getConnection(serverUrl, USER, password);
             Statement stmt = conn.createStatement()) {

            // Create database if not exists
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
            stmt.executeUpdate("USE " + DB_NAME);

            // Create users table matching requested schema:
            // Columns: user_id, name, email, password, role
            String createTableSQL = "CREATE TABLE IF NOT EXISTS users (" +
                    "user_id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(100) NOT NULL UNIQUE, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "role VARCHAR(50) NOT NULL" +
                    ")";
            stmt.executeUpdate(createTableSQL);

            // Seed initial sample users if table is empty
            var resultSet = stmt.executeQuery("SELECT COUNT(*) FROM users");
            if (resultSet.next() && resultSet.getInt(1) == 0) {
                stmt.executeUpdate("INSERT INTO users (name, email, password, role) VALUES " +
                        "('Alex Student', 'student@campus.edu', 'password123', 'Student'), " +
                        "('Jordan Coordinator', 'coord@campus.edu', 'password123', 'Coordinator'), " +
                        "('Sam Admin', 'admin@campus.edu', 'password123', 'Admin')");
            }
        }
    }
}
