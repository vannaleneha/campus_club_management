package com.campusclub.dao;

import com.campusclub.model.Club;
import com.campusclub.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Club operations.
 */
public class ClubDAO {

    /**
     * Retrieves all clubs that a specific user has joined.
     */
    public List<Club> getClubsForUser(int userId) throws SQLException {
        List<Club> userClubs = new ArrayList<>();
        String sql = "SELECT c.club_id, c.name, c.category, c.description, c.icon, cm.status " +
                     "FROM clubs c " +
                     "JOIN club_members cm ON c.club_id = cm.club_id " +
                     "WHERE cm.user_id = ? " +
                     "ORDER BY cm.joined_date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    userClubs.add(new Club(
                        rs.getInt("club_id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getString("description"),
                        rs.getString("icon"),
                        rs.getString("status") != null ? rs.getString("status") : "Active"
                    ));
                }
            }
        }
        return userClubs;
    }

    /**
     * Retrieves all campus clubs.
     */
    public List<Club> getAllClubs() throws SQLException {
        List<Club> allClubs = new ArrayList<>();
        String sql = "SELECT club_id, name, category, description, icon FROM clubs ORDER BY name ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                allClubs.add(new Club(
                    rs.getInt("club_id"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getString("description"),
                    rs.getString("icon"),
                    "Not Joined"
                ));
            }
        }
        return allClubs;
    }

    /**
     * Gets total count of clubs joined by a user.
     */
    public int getJoinedClubsCount(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM club_members WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Gets total member count for a specific club.
     */
    public int getClubMemberCount(int clubId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM club_members WHERE club_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, clubId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Joins a club if not already joined.
     */
    public boolean joinClub(int userId, int clubId) throws SQLException {
        String sql = "INSERT INTO club_members (user_id, club_id, status) VALUES (?, ?, 'Active') " +
                     "ON DUPLICATE KEY UPDATE status = 'Active'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, clubId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves the assigned club for a specific coordinator.
     */
    public Club getClubForCoordinator(int coordinatorId) throws SQLException {
        String sql = "SELECT club_id, name, category, description, icon FROM clubs WHERE coordinator_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, coordinatorId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Club(
                        rs.getInt("club_id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getString("description"),
                        rs.getString("icon"),
                        "Assigned"
                    );
                }
            }
        }

        // Fallback: Assign first club to coordinator if not explicitly linked
        String fallbackSql = "SELECT club_id, name, category, description, icon FROM clubs ORDER BY club_id ASC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(fallbackSql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                int clubId = rs.getInt("club_id");
                try (PreparedStatement updateStmt = conn.prepareStatement("UPDATE clubs SET coordinator_id = ? WHERE club_id = ?")) {
                    updateStmt.setInt(1, coordinatorId);
                    updateStmt.setInt(2, clubId);
                    updateStmt.executeUpdate();
                }
                return new Club(
                    clubId,
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getString("description"),
                    rs.getString("icon"),
                    "Assigned"
                );
            }
        }
        return null;
    }

    public int getClubsCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM clubs";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public static class ClubDetailRecord {
        public final int clubId;
        public final String name;
        public final String category;
        public final String description;
        public final String icon;
        public final Integer coordinatorId;
        public final String coordinatorName;

        public ClubDetailRecord(int clubId, String name, String category, String description, String icon, Integer coordinatorId, String coordinatorName) {
            this.clubId = clubId;
            this.name = name;
            this.category = category;
            this.description = description;
            this.icon = icon;
            this.coordinatorId = coordinatorId;
            this.coordinatorName = coordinatorName;
        }
    }

    public List<ClubDetailRecord> getAllClubsWithDetails() throws SQLException {
        List<ClubDetailRecord> list = new ArrayList<>();
        String sql = "SELECT c.club_id, c.name, c.category, c.description, c.icon, c.coordinator_id, " +
                     "u.name AS coord_name " +
                     "FROM clubs c " +
                     "LEFT JOIN users u ON c.coordinator_id = u.user_id " +
                     "ORDER BY c.name ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                int cId = rs.getInt("coordinator_id");
                Integer coordId = rs.wasNull() ? null : cId;
                list.add(new ClubDetailRecord(
                    rs.getInt("club_id"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getString("description"),
                    rs.getString("icon"),
                    coordId,
                    rs.getString("coord_name") != null ? rs.getString("coord_name") : "Unassigned"
                ));
            }
        }
        return list;
    }

    public boolean createClub(String name, String category, String description, Integer coordinatorId) throws SQLException {
        String sql = "INSERT INTO clubs (name, category, description, coordinator_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, category);
            stmt.setString(3, description);
            if (coordinatorId != null) {
                stmt.setInt(4, coordinatorId);
            } else {
                stmt.setNull(4, java.sql.Types.INTEGER);
            }
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateClub(int clubId, String name, String category, String description, Integer coordinatorId) throws SQLException {
        String sql = "UPDATE clubs SET name = ?, category = ?, description = ?, coordinator_id = ? WHERE club_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, category);
            stmt.setString(3, description);
            if (coordinatorId != null) {
                stmt.setInt(4, coordinatorId);
            } else {
                stmt.setNull(4, java.sql.Types.INTEGER);
            }
            stmt.setInt(5, clubId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deleteClub(int clubId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement s1 = conn.prepareStatement("DELETE FROM club_members WHERE club_id = ?")) {
                    s1.setInt(1, clubId);
                    s1.executeUpdate();
                }
                boolean deleted;
                try (PreparedStatement s2 = conn.prepareStatement("DELETE FROM clubs WHERE club_id = ?")) {
                    s2.setInt(1, clubId);
                    deleted = s2.executeUpdate() > 0;
                }
                conn.commit();
                return deleted;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
