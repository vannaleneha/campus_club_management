package com.campusclub.dao;

import com.campusclub.model.User;
import com.campusclub.util.DBConnection;
import com.campusclub.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for user authentication and user database queries.
 * Integrates BCrypt password hashing for secure authentication, verification, and migration.
 */
public class UserDAO {

    /**
     * Authenticates a user against the users table using PreparedStatement and BCrypt password verification.
     * Automatically migrates legacy plain-text passwords to secure BCrypt hashes upon successful login.
     *
     * @param emailOrRoll User's email or roll number (user_id).
     * @param password    User's candidate password.
     * @param role        User's selected role (Student, Coordinator, Admin).
     * @return User object if credentials and role match; null otherwise.
     * @throws SQLException If a database error occurs.
     */
    public User authenticateUser(String emailOrRoll, String password, String role) throws SQLException {
        String sql = "SELECT user_id, name, email, password, role FROM users " +
                     "WHERE (email = ? OR CAST(user_id AS CHAR) = ?) AND LOWER(role) = LOWER(?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, emailOrRoll);
            stmt.setString(2, emailOrRoll);
            stmt.setString(3, role);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");

                    if (PasswordUtil.checkPassword(password, storedPassword)) {
                        int userId = rs.getInt("user_id");

                        // Transparently migrate plain-text password to BCrypt hash
                        if (!PasswordUtil.isBCryptHash(storedPassword)) {
                            updatePassword(userId, password);
                        }

                        return new User(
                            userId,
                            rs.getString("name"),
                            rs.getString("email"),
                            storedPassword,
                            rs.getString("role")
                        );
                    }
                }
            }
        }

        return null; // Invalid credentials or role mismatch
    }

    /**
     * Verifies if candidate password matches logged-in user's stored password or BCrypt hash.
     */
    public boolean verifyPassword(int userId, String currentPassword) throws SQLException {
        String sql = "SELECT password FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    return PasswordUtil.checkPassword(currentPassword, storedPassword);
                }
            }
        }
        return false;
    }

    /**
     * Updates the specified user's password using BCrypt hashing via PreparedStatement.
     */
    public boolean updatePassword(int userId, String newPassword) throws SQLException {
        String sql = "UPDATE users SET password = ? WHERE user_id = ?";
        String hashedPassword = PasswordUtil.isBCryptHash(newPassword) ? newPassword : PasswordUtil.hashPassword(newPassword);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, hashedPassword);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    public int getStudentsCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE LOWER(role) = 'student'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public int getCoordinatorsCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE LOWER(role) = 'coordinator'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public static class StudentUserRecord {
        public final int userId;
        public final String name;
        public final String email;
        public final int joinedClubsCount;

        public StudentUserRecord(int userId, String name, String email, int joinedClubsCount) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.joinedClubsCount = joinedClubsCount;
        }
    }

    public List<StudentUserRecord> getAllStudents() throws SQLException {
        List<StudentUserRecord> list = new ArrayList<>();
        String sql = "SELECT u.user_id, u.name, u.email, " +
                     "(SELECT COUNT(*) FROM club_members cm WHERE cm.user_id = u.user_id) AS clubs_cnt " +
                     "FROM users u WHERE LOWER(u.role) = 'student' ORDER BY u.name ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new StudentUserRecord(
                    rs.getInt("user_id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getInt("clubs_cnt")
                ));
            }
        }
        return list;
    }

    public List<User> getAllCoordinators() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT user_id, name, email, password, role FROM users WHERE LOWER(role) = 'coordinator' ORDER BY name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new User(
                    rs.getInt("user_id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("role")
                ));
            }
        }
        return list;
    }

    public boolean createCoordinator(String name, String email, String password) throws SQLException {
        String sql = "INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, 'Coordinator')";
        String hashedPassword = PasswordUtil.isBCryptHash(password) ? password : PasswordUtil.hashPassword(password);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, hashedPassword);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateCoordinator(int userId, String name, String email) throws SQLException {
        String sql = "UPDATE users SET name = ?, email = ? WHERE user_id = ? AND LOWER(role) = 'coordinator'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setInt(3, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deleteCoordinator(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ? AND LOWER(role) = 'coordinator'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        }
    }
}
