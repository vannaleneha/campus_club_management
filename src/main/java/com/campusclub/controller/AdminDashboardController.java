package com.campusclub.controller;

import com.campusclub.dao.ClubDAO;
import com.campusclub.dao.ClubDAO.ClubDetailRecord;
import com.campusclub.dao.EventDAO;
import com.campusclub.dao.EventDAO.SystemEventRecord;
import com.campusclub.dao.EventDAO.SystemParticipationRecord;
import com.campusclub.dao.UserDAO;
import com.campusclub.dao.UserDAO.StudentUserRecord;
import com.campusclub.model.User;

import java.sql.SQLException;
import java.util.List;

/**
 * Controller mediating operations between AdminDashboardView and Data Access Objects.
 */
public class AdminDashboardController {

    private final ClubDAO clubDAO;
    private final EventDAO eventDAO;
    private final UserDAO userDAO;

    public AdminDashboardController() {
        this(new ClubDAO(), new EventDAO(), new UserDAO());
    }

    public AdminDashboardController(ClubDAO clubDAO, EventDAO eventDAO, UserDAO userDAO) {
        this.clubDAO = clubDAO;
        this.eventDAO = eventDAO;
        this.userDAO = userDAO;
    }

    public int getStudentsCount() throws SQLException {
        return userDAO.getStudentsCount();
    }

    public int getClubsCount() throws SQLException {
        return clubDAO.getClubsCount();
    }

    public int getTotalEventsCount() throws SQLException {
        return eventDAO.getTotalEventsCount();
    }

    public int getTotalAttendanceCount() throws SQLException {
        return eventDAO.getTotalAttendanceCount();
    }

    public List<ClubDetailRecord> getAllClubsWithDetails() throws SQLException {
        return clubDAO.getAllClubsWithDetails();
    }

    public boolean createClub(String name, String category, String description, Integer coordinatorId) throws SQLException {
        return clubDAO.createClub(name, category, description, coordinatorId);
    }

    public boolean updateClub(int clubId, String name, String category, String description, Integer coordinatorId) throws SQLException {
        return clubDAO.updateClub(clubId, name, category, description, coordinatorId);
    }

    public boolean deleteClub(int clubId) throws SQLException {
        return clubDAO.deleteClub(clubId);
    }

    public List<User> getAllCoordinators() throws SQLException {
        return userDAO.getAllCoordinators();
    }

    public boolean createCoordinator(String name, String email, String password) throws SQLException {
        return userDAO.createCoordinator(name, email, password);
    }

    public boolean updateCoordinator(int userId, String name, String email) throws SQLException {
        return userDAO.updateCoordinator(userId, name, email);
    }

    public boolean deleteCoordinator(int userId) throws SQLException {
        return userDAO.deleteCoordinator(userId);
    }

    public List<StudentUserRecord> getAllStudents() throws SQLException {
        return userDAO.getAllStudents();
    }

    public List<SystemEventRecord> getAllSystemEvents() throws SQLException {
        return eventDAO.getAllSystemEvents();
    }

    public List<SystemParticipationRecord> getSystemWideParticipation() throws SQLException {
        return eventDAO.getSystemWideParticipation();
    }

    public UserDAO getUserDAO() {
        return userDAO;
    }
}
