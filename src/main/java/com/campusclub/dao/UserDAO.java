package com.campusclub.dao;

import com.campusclub.model.User;
import com.campusclub.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for user authentication and user database queries.
 */
public class UserDAO {

    /**
     * Authenticates a user against the users table using PreparedStatement.
     *
     * @param emailOrRoll User's email or roll number (user_id).
     * @param password    User's password.
     * @param role        User's selected role (Student, Coordinator, Admin).
     * @return User object if credentials and role match; null otherwise.
     * @throws SQLException If a database error occurs.
     */
    public User authenticateUser(String emailOrRoll, String password, String role) throws SQLException {
        String sql = "SELECT user_id, name, email, password, role FROM users " +
                     "WHERE (email = ? OR CAST(user_id AS CHAR) = ?) AND password = ? AND LOWER(role) = LOWER(?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, emailOrRoll);
            stmt.setString(2, emailOrRoll);
            stmt.setString(3, password);
            stmt.setString(4, role);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getInt("user_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("role")
                    );
                }
            }
        }

        return null; // Invalid credentials or role mismatch
    }

    /**
     * Verifies if current password matches logged-in user's stored password.
     */
    public boolean verifyPassword(int userId, String currentPassword) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE user_id = ? AND password = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, currentPassword);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Updates only the specified logged-in user's password using PreparedStatement.
     */
    public boolean updatePassword(int userId, String newPassword) throws SQLException {
        String sql = "UPDATE users SET password = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newPassword);
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
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, password);
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
