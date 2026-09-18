package com.campusclub.model;

/**
 * Model class representing an Event in the Campus Club Management System.
 */
public class Event {

    private int eventId;
    private int clubId;
    private String title;
    private String eventDate;
    private String eventTime;
    private String location;
    private String description;
    private boolean registered;

    public Event() {
    }

    public Event(int eventId, int clubId, String title, String eventDate, String eventTime, String location, String description, boolean registered) {
        this.eventId = eventId;
        this.clubId = clubId;
        this.title = title;
        this.eventDate = eventDate;
        this.eventTime = eventTime;
        this.location = location;
        this.description = description;
        this.registered = registered;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public int getClubId() {
        return clubId;
    }

    public void setClubId(int clubId) {
        this.clubId = clubId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEventDate() {
        return eventDate;
    }

    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }

    public String getEventTime() {
        return eventTime;
    }

    public void setEventTime(String eventTime) {
        this.eventTime = eventTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }
}
