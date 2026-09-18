package com.campusclub.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * DBConnection manager for MySQL backend.
 * Reads database parameters from db.properties or system properties without hardcoding credentials.
 */
public class DBConnection {

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;

    private static Connection connection = null;

    static {
        loadProperties();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL Driver not found: " + e.getMessage());
        }
    }

    private static void loadProperties() {
        Properties props = new Properties();
        try (InputStream input = DBConnection.class.getResourceAsStream("/db.properties")) {
            if (input != null) {
                props.load(input);
            }
        } catch (Exception e) {
            System.err.println("Could not load db.properties: " + e.getMessage());
        }

        dbUrl = System.getProperty("db.url", props.getProperty("db.url", "jdbc:mysql://localhost:3306/campus_club?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"));
        dbUser = System.getProperty("db.user", props.getProperty("db.user", "root"));
        
        // Priority: -Ddb.password > DB_PASSWORD env var > db.properties
        dbPassword = System.getProperty("db.password");
        if (dbPassword == null) {
            dbPassword = System.getenv("DB_PASSWORD");
        }
        if (dbPassword == null) {
            dbPassword = props.getProperty("db.password", "");
        }
    }

    public static void setPassword(String password) {
        dbPassword = password;
        connection = null;
    }

    /**
     * Obtains an active connection to the MySQL database.
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            ensureTableAndUsers();
            connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        }
        return connection;
    }

    /**
     * Ensures the users table exists, contains ONLY the 3 specified student records (NEHA, CHARMI, PUJITHA),
     * and resets table data to match exact required state.
     */
    public static void ensureTableAndUsers() throws SQLException {
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             Statement stmt = conn.createStatement()) {

            // Create users table if not exists
            String createTableSQL = "CREATE TABLE IF NOT EXISTS users (" +
                    "user_id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(100) UNIQUE NOT NULL, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "role VARCHAR(20) NOT NULL" +
                    ")";
            stmt.executeUpdate(createTableSQL);

            // Check if admin user exists, insert if missing
            boolean adminExists = false;
            try (PreparedStatement adminStmt = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE LOWER(role) = 'admin'")) {
                try (ResultSet rs = adminStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        adminExists = true;
                    }
                }
            }

            if (!adminExists) {
                stmt.executeUpdate("INSERT INTO users (name, email, password, role) VALUES ('System Admin', 'admin@klu.ac.in', 'admin123', 'Admin')");
            }

            // Verify if exact student records exist
            boolean nehaExists = checkUserExists(conn, "99250040554@klu.ac.in", "NEHA");
            boolean charmiExists = checkUserExists(conn, "99250040558@klu.ac.in", "CHARMI");
            boolean pujithaExists = checkUserExists(conn, "99250040562@klu.ac.in", "PUJITHA");

            ResultSet countRs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            int totalCount = (countRs.next()) ? countRs.getInt(1) : 0;

            if (!nehaExists || !charmiExists || !pujithaExists) {
                if (!nehaExists) {
                    stmt.executeUpdate("INSERT INTO users (name, email, password, role) VALUES ('NEHA', '99250040554@klu.ac.in', '99250040554', 'Student')");
                }
                if (!charmiExists) {
                    stmt.executeUpdate("INSERT INTO users (name, email, password, role) VALUES ('CHARMI', '99250040558@klu.ac.in', '99250040558', 'Student')");
                }
                if (!pujithaExists) {
                    stmt.executeUpdate("INSERT INTO users (name, email, password, role) VALUES ('PUJITHA', '99250040562@klu.ac.in', '99250040562', 'Student')");
                }
            }

            ensureClubsAndEventsTables(stmt);
        }
    }

    private static void ensureClubsAndEventsTables(Statement stmt) throws SQLException {
        // 1. Create clubs table
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS clubs (" +
                "club_id INT PRIMARY KEY AUTO_INCREMENT, " +
                "coordinator_id INT DEFAULT NULL, " +
                "name VARCHAR(100) NOT NULL, " +
                "category VARCHAR(50) NOT NULL, " +
                "description TEXT, " +
                "icon VARCHAR(50) DEFAULT '🎭'" +
                ")");

        // Safely add coordinator_id column if clubs table was created in an earlier schema version
        try {
            stmt.executeUpdate("ALTER TABLE clubs ADD COLUMN coordinator_id INT DEFAULT NULL");
        } catch (SQLException ignored) {
            // Column already exists
        }

        // Seed or update clubs with exact requirements
        ResultSet clubsRs = stmt.executeQuery("SELECT COUNT(*) FROM clubs");
        if (clubsRs.next() && clubsRs.getInt(1) == 0) {
            stmt.executeUpdate("INSERT INTO clubs (club_id, name, category, description, icon) VALUES " +
                    "(1, 'SCRS', 'Technical', 'Encouraging undergraduate and postgraduate students to explore advanced computing domains and publish scientific papers.', '⚙️'), " +
                    "(2, 'IEEE SMC', 'Technical', 'Brainstorming and building real-world application prototypes during collaborative campus marathons.', '🤖'), " +
                    "(4, 'Vishaka', 'Non-Technical', 'The club acts as a creative hub dedicated to blending cultural tradition with innovative entertainment, fostering artistic expression among university students.', '🌸')");
        } else {
            // Update descriptions and details for existing records
            stmt.executeUpdate("UPDATE clubs SET name = 'SCRS', category = 'Technical', description = 'Encouraging undergraduate and postgraduate students to explore advanced computing domains and publish scientific papers.', icon = '⚙️' WHERE club_id = 1 OR name LIKE '%SCRS%'");
            stmt.executeUpdate("UPDATE clubs SET name = 'IEEE SMC', category = 'Technical', description = 'Brainstorming and building real-world application prototypes during collaborative campus marathons.', icon = '🤖' WHERE club_id = 2 OR name LIKE '%IEEE%' OR name LIKE '%SMC%'");
            stmt.executeUpdate("UPDATE clubs SET name = 'Vishaka', category = 'Non-Technical', description = 'The club acts as a creative hub dedicated to blending cultural tradition with innovative entertainment, fostering artistic expression among university students.', icon = '🌸' WHERE club_id = 4 OR name LIKE '%Vishaka%'");
            // Remove obsolete placeholder/duplicate rows if present
            stmt.executeUpdate("DELETE FROM clubs WHERE name NOT IN ('SCRS', 'IEEE SMC', 'Vishaka')");
        }

        // 2. Create club_members table
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS club_members (" +
                "member_id INT PRIMARY KEY AUTO_INCREMENT, " +
                "user_id INT NOT NULL, " +
                "club_id INT NOT NULL, " +
                "status VARCHAR(20) DEFAULT 'Active', " +
                "joined_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE KEY unique_user_club (user_id, club_id)" +
                ")");

        // 3. Create events table
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS events (" +
                "event_id INT PRIMARY KEY AUTO_INCREMENT, " +
                "club_id INT, " +
                "title VARCHAR(100) NOT NULL, " +
                "event_date VARCHAR(50) NOT NULL, " +
                "event_time VARCHAR(50) NOT NULL, " +
                "location VARCHAR(100) NOT NULL, " +
                "description TEXT, " +
                "status VARCHAR(20) DEFAULT 'Upcoming'" +
                ")");

        // 4. Create event_registrations table
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS event_registrations (" +
                "registration_id INT PRIMARY KEY AUTO_INCREMENT, " +
                "user_id INT NOT NULL, " +
                "event_id INT NOT NULL, " +
                "status VARCHAR(20) DEFAULT 'Registered', " +
                "registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE KEY unique_user_event (user_id, event_id)" +
                ")");

        // 5. Create attendance table
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS attendance (" +
                "attendance_id INT PRIMARY KEY AUTO_INCREMENT, " +
                "event_id INT NOT NULL, " +
                "user_id INT NOT NULL, " +
                "status VARCHAR(20) DEFAULT 'Present', " +
                "marked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE KEY unique_event_user (event_id, user_id)" +
                ")");
    }

    private static boolean checkUserExists(Connection conn, String email, String name) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ? AND name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, name);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}
