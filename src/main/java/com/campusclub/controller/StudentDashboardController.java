package com.campusclub.controller;

import com.campusclub.dao.ClubDAO;
import com.campusclub.dao.EventDAO;
import com.campusclub.dao.UserDAO;
import com.campusclub.model.Club;
import com.campusclub.model.Event;

import java.sql.SQLException;
import java.util.List;

/**
 * Controller mediating operations between StudentDashboardView and Data Access Objects.
 */
public class StudentDashboardController {

    private final ClubDAO clubDAO;
    private final EventDAO eventDAO;
    private final UserDAO userDAO;

    public StudentDashboardController() {
        this(new ClubDAO(), new EventDAO(), new UserDAO());
    }

    public StudentDashboardController(ClubDAO clubDAO, EventDAO eventDAO, UserDAO userDAO) {
        this.clubDAO = clubDAO;
        this.eventDAO = eventDAO;
        this.userDAO = userDAO;
    }

    public int getJoinedClubsCount(int userId) throws SQLException {
        return clubDAO.getJoinedClubsCount(userId);
    }

    public int getUpcomingEventsCount() throws SQLException {
        return eventDAO.getUpcomingEventsCount();
    }

    public int getEventsAttendedCount(int userId) throws SQLException {
        return eventDAO.getEventsAttendedCount(userId);
    }

    public double getParticipationRate(int userId) throws SQLException {
        return eventDAO.getParticipationRate(userId);
    }

    public List<Club> getClubsForUser(int userId) throws SQLException {
        return clubDAO.getClubsForUser(userId);
    }

    public List<Club> getAllClubs() throws SQLException {
        return clubDAO.getAllClubs();
    }

    public int getClubMemberCount(int clubId) throws SQLException {
        return clubDAO.getClubMemberCount(clubId);
    }

    public boolean joinClub(int userId, int clubId) throws SQLException {
        return clubDAO.joinClub(userId, clubId);
    }

    public List<Event> getUpcomingEventsForUser(int userId) throws SQLException {
        return eventDAO.getUpcomingEventsForUser(userId);
    }

    public boolean registerForEvent(int userId, int eventId) throws SQLException {
        return eventDAO.registerForEvent(userId, eventId);
    }

    public List<Event> getUserRegisteredEvents(int userId) throws SQLException {
        return eventDAO.getUserRegisteredEvents(userId);
    }

    public UserDAO getUserDAO() {
        return userDAO;
    }

    public ClubDAO getClubDAO() {
        return clubDAO;
    }

    public EventDAO getEventDAO() {
        return eventDAO;
    }
}
