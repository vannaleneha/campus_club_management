package com.campusclub.model;

/**
 * Model class representing a Club in the Campus Club Management System.
 */
public class Club {

    private int clubId;
    private String name;
    private String category;
    private String description;
    private String icon;
    private String membershipStatus;

    public Club() {
    }

    public Club(int clubId, String name, String category, String description, String icon, String membershipStatus) {
        this.clubId = clubId;
        this.name = name;
        this.category = category;
        this.description = description;
        this.icon = icon;
        this.membershipStatus = membershipStatus;
    }

    public int getClubId() {
        return clubId;
    }

    public void setClubId(int clubId) {
        this.clubId = clubId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getMembershipStatus() {
        return membershipStatus;
    }

    public void setMembershipStatus(String membershipStatus) {
        this.membershipStatus = membershipStatus;
    }
}
