package com.campusclub.dao;

import com.campusclub.model.Event;
import com.campusclub.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Event operations and event registrations.
 */
public class EventDAO {

    /**
     * Gets all upcoming events and indicates whether the specified user is registered.
     */
    public List<Event> getUpcomingEventsForUser(int userId) throws SQLException {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT e.event_id, e.club_id, e.title, e.event_date, e.event_time, e.location, e.description, " +
                     "(SELECT COUNT(*) FROM event_registrations er WHERE er.event_id = e.event_id AND er.user_id = ?) AS is_reg " +
                     "FROM events e " +
                     "WHERE e.status = 'Upcoming' " +
                     "ORDER BY e.event_id ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    events.add(new Event(
                        rs.getInt("event_id"),
                        rs.getInt("club_id"),
                        rs.getString("title"),
                        rs.getString("event_date"),
                        rs.getString("event_time"),
                        rs.getString("location"),
                        rs.getString("description"),
                        rs.getInt("is_reg") > 0
                    ));
                }
            }
        }
        return events;
    }

    /**
     * Gets total number of upcoming events in the system.
     */
    public int getUpcomingEventsCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM events WHERE status = 'Upcoming'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Gets total number of events attended by the student.
     */
    public int getEventsAttendedCount(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM event_registrations WHERE user_id = ? AND status = 'Attended'";
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
     * Gets the participation percentage for the student.
     */
    public double getParticipationRate(int userId) throws SQLException {
        int totalEvents = getUpcomingEventsCount();
        if (totalEvents == 0) return 0.0;

        String sql = "SELECT COUNT(*) FROM event_registrations WHERE user_id = ?";
        int studentRegistrations = 0;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    studentRegistrations = rs.getInt(1);
                }
            }
        }

        return Math.min(100.0, ((double) studentRegistrations / totalEvents) * 100.0);
    }

    /**
     * Registers a student for an upcoming event, preventing duplicate registration.
     */
    public boolean registerForEvent(int userId, int eventId) throws SQLException {
        if (isUserRegisteredForEvent(userId, eventId)) {
            return false; // Already registered
        }

        String sql = "INSERT INTO event_registrations (user_id, event_id, status) VALUES (?, ?, 'Registered')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, eventId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Checks whether a student is registered for a specific event.
     */
    public boolean isUserRegisteredForEvent(int userId, int eventId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM event_registrations WHERE user_id = ? AND event_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, eventId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Gets all registered/attended events for a specific student.
     */
    public List<Event> getUserRegisteredEvents(int userId) throws SQLException {
        List<Event> userEvents = new ArrayList<>();
        String sql = "SELECT e.event_id, e.club_id, e.title, e.event_date, e.event_time, e.location, e.description, er.status " +
                     "FROM events e " +
                     "JOIN event_registrations er ON e.event_id = er.event_id " +
                     "WHERE er.user_id = ? " +
                     "ORDER BY er.registered_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Event evt = new Event(
                        rs.getInt("event_id"),
                        rs.getInt("club_id"),
                        rs.getString("title"),
                        rs.getString("event_date"),
                        rs.getString("event_time"),
                        rs.getString("location"),
                        rs.getString("description"),
                        true
                    );
                    userEvents.add(evt);
                }
            }
        }
        return userEvents;
    }

    /**
     * Retrieves all events belonging to a specific club.
     */
    public List<Event> getEventsByClub(int clubId) throws SQLException {
        List<Event> clubEvents = new ArrayList<>();
        String sql = "SELECT e.event_id, e.club_id, e.title, e.event_date, e.event_time, e.location, e.description, " +
                     "(SELECT COUNT(*) FROM event_registrations er WHERE er.event_id = e.event_id) AS reg_count " +
                     "FROM events e WHERE e.club_id = ? ORDER BY e.event_id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, clubId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Event event = new Event(
                        rs.getInt("event_id"),
                        rs.getInt("club_id"),
                        rs.getString("title"),
                        rs.getString("event_date"),
                        rs.getString("event_time"),
                        rs.getString("location"),
                        rs.getString("description"),
                        false
                    );
                    clubEvents.add(event);
                }
            }
        }
        return clubEvents;
    }

    public int getEventsCountForClub(int clubId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM events WHERE club_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, clubId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public int getRegistrationsCountForClub(int clubId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM event_registrations er JOIN events e ON er.event_id = e.event_id WHERE e.club_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, clubId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public int getAttendanceCountForClub(int clubId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM attendance a JOIN events e ON a.event_id = e.event_id WHERE e.club_id = ? AND a.status = 'Present'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, clubId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public boolean createEvent(int clubId, String title, String date, String time, String location, String description) throws SQLException {
        String sql = "INSERT INTO events (club_id, title, event_date, event_time, location, description, status) VALUES (?, ?, ?, ?, ?, ?, 'Upcoming')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, clubId);
            stmt.setString(2, title);
            stmt.setString(3, date);
            stmt.setString(4, time);
            stmt.setString(5, location);
            stmt.setString(6, description);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateEvent(int eventId, String title, String date, String time, String location, String description) throws SQLException {
        String sql = "UPDATE events SET title = ?, event_date = ?, event_time = ?, location = ?, description = ? WHERE event_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, title);
            stmt.setString(2, date);
            stmt.setString(3, time);
            stmt.setString(4, location);
            stmt.setString(5, description);
            stmt.setInt(6, eventId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deleteEvent(int eventId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement s1 = conn.prepareStatement("DELETE FROM attendance WHERE event_id = ?")) {
                    s1.setInt(1, eventId);
                    s1.executeUpdate();
                }
                try (PreparedStatement s2 = conn.prepareStatement("DELETE FROM event_registrations WHERE event_id = ?")) {
                    s2.setInt(1, eventId);
                    s2.executeUpdate();
                }
                boolean deleted;
                try (PreparedStatement s3 = conn.prepareStatement("DELETE FROM events WHERE event_id = ?")) {
                    s3.setInt(1, eventId);
                    deleted = s3.executeUpdate() > 0;
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

    public static class StudentRegistration {
        public final int userId;
        public final String name;
        public final String email;
        public final String registeredAt;
        public final String status;
        public String attendanceStatus;

        public StudentRegistration(int userId, String name, String email, String registeredAt, String status, String attendanceStatus) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.registeredAt = registeredAt;
            this.status = status;
            this.attendanceStatus = attendanceStatus;
        }
    }

    public List<StudentRegistration> getRegistrationsForEvent(int eventId) throws SQLException {
        List<StudentRegistration> list = new ArrayList<>();
        String sql = "SELECT u.user_id, u.name, u.email, er.registered_at, er.status, " +
                     "COALESCE(a.status, 'Not Marked') AS att_status " +
                     "FROM event_registrations er " +
                     "JOIN users u ON er.user_id = u.user_id " +
                     "LEFT JOIN attendance a ON (a.event_id = er.event_id AND a.user_id = er.user_id) " +
                     "WHERE er.event_id = ? " +
                     "ORDER BY u.name ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new StudentRegistration(
                        rs.getInt("user_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("registered_at") != null ? rs.getString("registered_at").toString() : "N/A",
                        rs.getString("status"),
                        rs.getString("att_status")
                    ));
                }
            }
        }
        return list;
    }

    public boolean saveAttendance(int eventId, int userId, String status) throws SQLException {
        String sql = "INSERT INTO attendance (event_id, user_id, status) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE status = VALUES(status), marked_at = CURRENT_TIMESTAMP";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            stmt.setInt(2, userId);
            stmt.setString(3, status);
            int rows = stmt.executeUpdate();

            if ("Present".equalsIgnoreCase(status) || "Attended".equalsIgnoreCase(status)) {
                try (PreparedStatement updateReg = conn.prepareStatement("UPDATE event_registrations SET status = 'Attended' WHERE event_id = ? AND user_id = ?")) {
                    updateReg.setInt(1, eventId);
                    updateReg.setInt(2, userId);
                    updateReg.executeUpdate();
                }
            } else {
                try (PreparedStatement updateReg = conn.prepareStatement("UPDATE event_registrations SET status = 'Registered' WHERE event_id = ? AND user_id = ?")) {
                    updateReg.setInt(1, eventId);
                    updateReg.setInt(2, userId);
                    updateReg.executeUpdate();
                }
            }
            return rows > 0;
        }
    }

    public static class StudentParticipation {
        public final String name;
        public final int registeredCount;
        public final int attendedCount;
        public final double percentage;

        public StudentParticipation(String name, int registeredCount, int attendedCount, double percentage) {
            this.name = name;
            this.registeredCount = registeredCount;
            this.attendedCount = attendedCount;
            this.percentage = percentage;
        }
    }

    public List<StudentParticipation> getParticipationForClub(int clubId) throws SQLException {
        List<StudentParticipation> list = new ArrayList<>();
        String sql = "SELECT u.name, " +
                     "COUNT(er.registration_id) AS reg_cnt, " +
                     "SUM(CASE WHEN a.status = 'Present' OR er.status = 'Attended' THEN 1 ELSE 0 END) AS att_cnt " +
                     "FROM event_registrations er " +
                     "JOIN events e ON er.event_id = e.event_id " +
                     "JOIN users u ON er.user_id = u.user_id " +
                     "LEFT JOIN attendance a ON (a.event_id = e.event_id AND a.user_id = u.user_id) " +
                     "WHERE e.club_id = ? " +
                     "GROUP BY u.user_id, u.name " +
                     "ORDER BY u.name ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, clubId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int reg = rs.getInt("reg_cnt");
                    int att = rs.getInt("att_cnt");
                    double pct = reg > 0 ? (double) att / reg * 100.0 : 0.0;
                    list.add(new StudentParticipation(rs.getString("name"), reg, att, pct));
                }
            }
        }
        return list;
    }

    public int getTotalEventsCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM events";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public int getTotalAttendanceCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM attendance WHERE status = 'Present'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public static class SystemEventRecord {
        public final int eventId;
        public final String clubName;
        public final String title;
        public final String date;
        public final String time;
        public final String location;
        public final int regCount;

        public SystemEventRecord(int eventId, String clubName, String title, String date, String time, String location, int regCount) {
            this.eventId = eventId;
            this.clubName = clubName;
            this.title = title;
            this.date = date;
            this.time = time;
            this.location = location;
            this.regCount = regCount;
        }
    }

    public List<SystemEventRecord> getAllSystemEvents() throws SQLException {
        List<SystemEventRecord> list = new ArrayList<>();
        String sql = "SELECT e.event_id, c.name AS club_name, e.title, e.event_date, e.event_time, e.location, " +
                     "(SELECT COUNT(*) FROM event_registrations er WHERE er.event_id = e.event_id) AS reg_cnt " +
                     "FROM events e " +
                     "LEFT JOIN clubs c ON e.club_id = c.club_id " +
                     "ORDER BY e.event_id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new SystemEventRecord(
                    rs.getInt("event_id"),
                    rs.getString("club_name") != null ? rs.getString("club_name") : "General",
                    rs.getString("title"),
                    rs.getString("event_date"),
                    rs.getString("event_time"),
                    rs.getString("location"),
                    rs.getInt("reg_cnt")
                ));
            }
        }
        return list;
    }

    public static class SystemParticipationRecord {
        public final String studentName;
        public final String clubName;
        public final int registeredCount;
        public final int attendedCount;
        public final double percentage;

        public SystemParticipationRecord(String studentName, String clubName, int registeredCount, int attendedCount, double percentage) {
            this.studentName = studentName;
            this.clubName = clubName;
            this.registeredCount = registeredCount;
            this.attendedCount = attendedCount;
            this.percentage = percentage;
        }
    }

    public List<SystemParticipationRecord> getSystemWideParticipation() throws SQLException {
        List<SystemParticipationRecord> list = new ArrayList<>();
        String sql = "SELECT u.name AS student_name, COALESCE(c.name, 'General') AS club_name, " +
                     "COUNT(er.registration_id) AS reg_cnt, " +
                     "SUM(CASE WHEN a.status = 'Present' OR er.status = 'Attended' THEN 1 ELSE 0 END) AS att_cnt " +
                     "FROM event_registrations er " +
                     "JOIN events e ON er.event_id = e.event_id " +
                     "LEFT JOIN clubs c ON e.club_id = c.club_id " +
                     "JOIN users u ON er.user_id = u.user_id " +
                     "LEFT JOIN attendance a ON (a.event_id = e.event_id AND a.user_id = u.user_id) " +
                     "GROUP BY u.user_id, u.name, c.club_id, c.name " +
                     "ORDER BY u.name ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                int reg = rs.getInt("reg_cnt");
                int att = rs.getInt("att_cnt");
                double pct = reg > 0 ? (double) att / reg * 100.0 : 0.0;
                list.add(new SystemParticipationRecord(
                    rs.getString("student_name"),
                    rs.getString("club_name"),
                    reg,
                    att,
                    pct
                ));
            }
        }
        return list;
    }
}
