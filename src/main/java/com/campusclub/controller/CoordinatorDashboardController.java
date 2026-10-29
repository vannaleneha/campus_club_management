package com.campusclub.controller;

import com.campusclub.dao.ClubDAO;
import com.campusclub.dao.EventDAO;
import com.campusclub.dao.EventDAO.StudentParticipation;
import com.campusclub.dao.EventDAO.StudentRegistration;
import com.campusclub.dao.UserDAO;
import com.campusclub.model.Club;
import com.campusclub.model.Event;

import java.sql.SQLException;
import java.util.List;

/**
 * Controller mediating operations between CoordinatorDashboardView and Data Access Objects.
 */
public class CoordinatorDashboardController {

    private final ClubDAO clubDAO;
    private final EventDAO eventDAO;
    private final UserDAO userDAO;

    public CoordinatorDashboardController() {
        this(new ClubDAO(), new EventDAO(), new UserDAO());
    }

    public CoordinatorDashboardController(ClubDAO clubDAO, EventDAO eventDAO, UserDAO userDAO) {
        this.clubDAO = clubDAO;
        this.eventDAO = eventDAO;
        this.userDAO = userDAO;
    }

    public Club getAssignedClub(int coordinatorId) throws SQLException {
        return clubDAO.getClubForCoordinator(coordinatorId);
    }

    public int getEventsCountForClub(int clubId) throws SQLException {
        return eventDAO.getEventsCountForClub(clubId);
    }

    public int getRegistrationsCountForClub(int clubId) throws SQLException {
        return eventDAO.getRegistrationsCountForClub(clubId);
    }

    public int getAttendanceCountForClub(int clubId) throws SQLException {
        return eventDAO.getAttendanceCountForClub(clubId);
    }

    public List<Event> getEventsByClub(int clubId) throws SQLException {
        return eventDAO.getEventsByClub(clubId);
    }

    public boolean createEvent(int clubId, String title, String date, String time, String location, String description) throws SQLException {
        return eventDAO.createEvent(clubId, title, date, time, location, description);
    }

    public boolean updateEvent(int eventId, String title, String date, String time, String location, String description) throws SQLException {
        return eventDAO.updateEvent(eventId, title, date, time, location, description);
    }

    public boolean deleteEvent(int eventId) throws SQLException {
        return eventDAO.deleteEvent(eventId);
    }

    public List<StudentRegistration> getRegistrationsForEvent(int eventId) throws SQLException {
        return eventDAO.getRegistrationsForEvent(eventId);
    }

    public boolean saveAttendance(int eventId, int userId, String status) throws SQLException {
        return eventDAO.saveAttendance(eventId, userId, status);
    }

    public List<StudentParticipation> getParticipationForClub(int clubId) throws SQLException {
        return eventDAO.getParticipationForClub(clubId);
    }

    public UserDAO getUserDAO() {
        return userDAO;
    }
}
