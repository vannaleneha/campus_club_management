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
 * Reads database parameters from system properties, environment variables, or db.properties.
 * Ensures database schema, BCrypt-hashed baseline users, and initial seeds are initialized once on startup.
 */
public class DBConnection {

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;

    private static volatile boolean schemaInitialized = false;

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

        // Priority: -Ddb.url > DB_URL env var > db.properties > sensible default
        dbUrl = System.getProperty("db.url");
        if (dbUrl == null || dbUrl.trim().isEmpty()) {
            dbUrl = System.getenv("DB_URL");
        }
        if (dbUrl == null || dbUrl.trim().isEmpty()) {
            dbUrl = props.getProperty("db.url", "jdbc:mysql://localhost:3306/campus_club?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        }

        // Priority: -Ddb.user > DB_USER env var > db.properties > default root
        dbUser = System.getProperty("db.user");
        if (dbUser == null || dbUser.trim().isEmpty()) {
            dbUser = System.getenv("DB_USER");
        }
        if (dbUser == null || dbUser.trim().isEmpty()) {
            dbUser = props.getProperty("db.user", "root");
        }

        // Priority: -Ddb.password > DB_PASSWORD env var > db.properties > default empty
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
        schemaInitialized = false;
    }

    /**
     * Obtains an active connection to the MySQL database.
     * Ensures schema initialization is performed once before returning connections.
     */
    public static Connection getConnection() throws SQLException {
        ensureDatabaseInitialized();
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    /**
     * Thread-safe one-time initialization of database tables and initial baseline seeds.
     */
    public static synchronized void ensureDatabaseInitialized() throws SQLException {
        if (!schemaInitialized) {
            ensureTableAndUsers();
            schemaInitialized = true;
        }
    }

    /**
     * Ensures required tables (users, clubs, club_members, events, event_registrations, attendance)
     * and default seed users exist with BCrypt-hashed passwords.
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

            // Check if admin user exists, insert with BCrypt hash if missing
            boolean adminExists = false;
            try (PreparedStatement adminStmt = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE LOWER(role) = 'admin'")) {
                try (ResultSet rs = adminStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        adminExists = true;
                    }
                }
            }
            if (!adminExists) {
                insertSeedUser(conn, "System Admin", "admin@klu.ac.in", "admin123", "Admin");
            }

            // Check if default coordinator exists, insert with BCrypt hash if missing for instant testing
            boolean coordExists = false;
            try (PreparedStatement coordStmt = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE LOWER(role) = 'coordinator'")) {
                try (ResultSet rs = coordStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        coordExists = true;
                    }
                }
            }
            if (!coordExists) {
                insertSeedUser(conn, "Jordan Coordinator", "coord@klu.ac.in", "coord123", "Coordinator");
            }

            // Verify if exact student records exist, insert with BCrypt hash if missing
            boolean nehaExists = checkUserExists(conn, "99250040554@klu.ac.in", "NEHA");
            boolean charmiExists = checkUserExists(conn, "99250040558@klu.ac.in", "CHARMI");
            boolean pujithaExists = checkUserExists(conn, "99250040562@klu.ac.in", "PUJITHA");

            if (!nehaExists) {
                insertSeedUser(conn, "NEHA", "99250040554@klu.ac.in", "99250040554", "Student");
            }
            if (!charmiExists) {
                insertSeedUser(conn, "CHARMI", "99250040558@klu.ac.in", "99250040558", "Student");
            }
            if (!pujithaExists) {
                insertSeedUser(conn, "PUJITHA", "99250040562@klu.ac.in", "99250040562", "Student");
            }

            ensureClubsAndEventsTables(stmt);
        }
    }

    private static void insertSeedUser(Connection conn, String name, String email, String plainPassword, String role) throws SQLException {
        String sql = "INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, PasswordUtil.hashPassword(plainPassword));
            stmt.setString(4, role);
            stmt.executeUpdate();
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

        // Seed clubs if empty
        ResultSet clubsRs = stmt.executeQuery("SELECT COUNT(*) FROM clubs");
        if (clubsRs.next() && clubsRs.getInt(1) == 0) {
            stmt.executeUpdate("INSERT INTO clubs (club_id, name, category, description, icon) VALUES " +
                    "(1, 'SCRS', 'Technical', 'Encouraging undergraduate and postgraduate students to explore advanced computing domains and publish scientific papers.', '⚙️'), " +
                    "(2, 'IEEE SMC', 'Technical', 'Brainstorming and building real-world application prototypes during collaborative campus marathons.', '🤖'), " +
                    "(4, 'Vishaka', 'Non-Technical', 'The club acts as a creative hub dedicated to blending cultural tradition with innovative entertainment, fostering artistic expression among university students.', '🌸')");
        } else {
            // Update descriptions and details for default clubs without deleting user-created clubs
            stmt.executeUpdate("UPDATE clubs SET name = 'SCRS', category = 'Technical', description = 'Encouraging undergraduate and postgraduate students to explore advanced computing domains and publish scientific papers.', icon = '⚙️' WHERE club_id = 1 OR name LIKE '%SCRS%'");
            stmt.executeUpdate("UPDATE clubs SET name = 'IEEE SMC', category = 'Technical', description = 'Brainstorming and building real-world application prototypes during collaborative campus marathons.', icon = '🤖' WHERE club_id = 2 OR name LIKE '%IEEE%' OR name LIKE '%SMC%'");
            stmt.executeUpdate("UPDATE clubs SET name = 'Vishaka', category = 'Non-Technical', description = 'The club acts as a creative hub dedicated to blending cultural tradition with innovative entertainment, fostering artistic expression among university students.', icon = '🌸' WHERE club_id = 4 OR name LIKE '%Vishaka%'");
        }

        // Link default coordinator to club 1 if unassigned
        try {
            stmt.executeUpdate("UPDATE clubs SET coordinator_id = (SELECT user_id FROM users WHERE email = 'coord@klu.ac.in' LIMIT 1) WHERE club_id = 1 AND coordinator_id IS NULL");
        } catch (SQLException ignored) {
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

        // Seed initial sample events if events table is empty
        ResultSet eventsRs = stmt.executeQuery("SELECT COUNT(*) FROM events");
        if (eventsRs.next() && eventsRs.getInt(1) == 0) {
            stmt.executeUpdate("INSERT INTO events (club_id, title, event_date, event_time, location, description, status) VALUES " +
                    "(1, 'Soft Computing & AI Workshop', 'Nov 15, 2026', '10:00 AM', 'Room 301, Engineering Block', 'Hands-on training session on neural networks, fuzzy systems, and computing applications.', 'Upcoming'), " +
                    "(2, 'Autonomous Systems Hackathon', 'Nov 20, 2026', '09:00 AM', 'Innovation Lab 2', 'Collaborative 24-hour sprint prototyping cybernetics and IoT solutions.', 'Upcoming'), " +
                    "(4, 'Vishaka Cultural Orientation', 'Nov 28, 2026', '03:00 PM', 'Main Auditorium', 'Annual welcome and performing arts showcase celebrating university creative talents.', 'Upcoming')");
        }

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
