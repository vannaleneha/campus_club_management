package com.campusclub.service;

import com.campusclub.dao.ClubDAO;
import com.campusclub.dao.EventDAO;
import com.campusclub.dao.EventDAO.SystemEventRecord;
import com.campusclub.model.Club;
import com.campusclub.model.Event;
import com.campusclub.model.User;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intelligent, Database-Connected Chatbot Service for CCMS.
 * Grounds answers in live MySQL data, recognizes natural-language variations,
 * preserves student privacy, maintains conversational context, and answers project questions.
 */
public class ChatbotService {

    private final ClubDAO clubDAO;
    private final EventDAO eventDAO;

    // Conversational Context State
    private String lastTopic = null; // "CLUBS", "EVENTS", "CLUB_DETAIL"
    private List<Club> lastClubList = new ArrayList<>();
    private List<SystemEventRecord> lastEventList = new ArrayList<>();
    private Club lastDiscussedClub = null;

    public ChatbotService() {
        this(new ClubDAO(), new EventDAO());
    }

    public ChatbotService(ClubDAO clubDAO, EventDAO eventDAO) {
        this.clubDAO = clubDAO;
        this.eventDAO = eventDAO;
    }

    /**
     * Primary entry point to process a user question and generate a grounded, intelligent answer.
     *
     * @param message     the question entered by the user
     * @param currentUser the currently logged-in user (null if anonymous/unauthenticated)
     * @return the assistant's formatted response string
     */
    public String getResponse(String message, User currentUser) {
        if (message == null || message.trim().isEmpty()) {
            return "Please type a question so I can assist you! You can ask about campus clubs, upcoming events, registrations, or your attendance records.";
        }

        String raw = message.trim();
        String q = normalize(raw);

        try {
            // 1. Security & Privacy: Guard against student snooping on others
            if (isPrivacyViolation(q, raw, currentUser)) {
                return handlePrivacyViolation();
            }

            // 2. Conversational Follow-up questions
            if (isFollowUpQuery(q)) {
                String followUpResponse = handleFollowUp(q);
                if (followUpResponse != null) {
                    return followUpResponse;
                }
            }

            // 3. Specific Club Details by Name (e.g. "Tell me about SCRS", "What is IEEE SMC?")
            if (isSpecificClubInquiry(q)) {
                return handleSpecificClubInquiry(q, raw);
            }

            // 4. Club Listing & Filtering
            if (isTechnicalClubsQuery(q)) {
                return handleClubsList("Technical");
            }
            if (isNonTechnicalClubsQuery(q)) {
                return handleClubsList("Non-Technical");
            }
            if (isGeneralClubsListQuery(q)) {
                return handleClubsList(null);
            }

            // 5. How to Join a Club
            if (isHowToJoinClubQuery(q)) {
                return handleHowToJoinClub();
            }

            // 6. Student's Joined Clubs
            if (isMyClubsQuery(q)) {
                return handleMyJoinedClubs(currentUser);
            }

            // 7. Event Listing & Upcoming Events
            if (isEventsListQuery(q)) {
                return handleUpcomingEvents();
            }

            // 8. How to Register for an Event
            if (isHowToRegisterEventQuery(q)) {
                return handleHowToRegisterEvent();
            }

            // 9. Student's Registered Events & Participation Status
            if (isMyRegisteredEventsQuery(q)) {
                return handleMyRegisteredEvents(currentUser);
            }
            if (isMyParticipationQuery(q)) {
                return handleMyParticipation(currentUser);
            }

            // 10. Project Knowledge Base (CCMS, Tech Stack, Login, JDBC, Storage/Schema)
            if (isCcmsOverviewQuery(q)) {
                return handleCcmsOverview();
            }
            if (isTechStackQuery(q)) {
                return handleTechStack();
            }
            if (isLoginQuery(q)) {
                return handleLoginExplanation();
            }
            if (isJdbcQuery(q)) {
                return handleJdbcExplanation();
            }
            if (isStorageQuery(q)) {
                return handleStorageExplanation();
            }
            if (isChangePasswordQuery(q)) {
                return handleChangePasswordExplanation();
            }
            if (isChatbotCapabilitiesQuery(q)) {
                return handleChatbotCapabilities();
            }

            // 11. Generic fallback for questions outside CCMS domain
            return handleFallback(raw);

        } catch (SQLException e) {
            return "⚠️ Database Notice: An error occurred while retrieving live information: " + e.getMessage() + ". Please check your database connection or try again.";
        }
    }

    /**
     * Backward-compatible overload without user context.
     */
    public String getResponse(String message) {
        return getResponse(message, null);
    }

    // =========================================================================
    // NATURAL LANGUAGE INTENT CLASSIFIERS
    // =========================================================================

    private String normalize(String s) {
        return s.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isPrivacyViolation(String q, String raw, User currentUser) {
        // Known seed student names or general patterns: "neha", "charmi", "pujitha", roll numbers, "other student"
        String currentName = currentUser != null && currentUser.getName() != null ? currentUser.getName().toLowerCase() : "";
        String currentEmail = currentUser != null && currentUser.getEmail() != null ? currentUser.getEmail().toLowerCase() : "";

        String[] sensitiveKeywords = {"attendance", "events", "registered", "participation", "clubs", "details", "record", "password"};
        boolean mentionsSensitive = false;
        for (String k : sensitiveKeywords) {
            if (q.contains(k)) {
                mentionsSensitive = true;
                break;
            }
        }

        if (!mentionsSensitive) return false;

        // Check if query targets other known students by name
        String[] otherStudents = {"neha", "charmi", "pujitha"};
        for (String other : otherStudents) {
            if (q.contains(other) && !currentName.contains(other)) {
                return true;
            }
        }

        // Check if query targets another student's roll number (e.g. 99250040554)
        Pattern rollPattern = Pattern.compile("\\b99\\d{9}\\b");
        Matcher m = rollPattern.matcher(raw);
        if (m.find()) {
            String roll = m.group();
            if (!currentEmail.contains(roll)) {
                return true;
            }
        }

        // Check for phrases like "another student", "other student", "someone else", "user 2"
        return q.contains("other student") || q.contains("another student") || q.contains("someone else")
                || q.contains("other users") || q.contains("different student") || q.contains("user 2")
                || q.contains("user 3") || q.contains("classmate");
    }

    private boolean isFollowUpQuery(String q) {
        return q.contains("first one") || q.contains("first club") || q.contains("second one")
                || q.contains("second club") || q.contains("third one") || q.contains("third club")
                || q.equals("tell me more") || q.equals("tell me more about it") || q.equals("more details")
                || q.equals("what about it");
    }

    private boolean isSpecificClubInquiry(String q) {
        if (q.contains("my club") || q.contains("how to join") || q.contains("join a club") || q.contains("all clubs") || q.contains("which clubs can i join")) {
            return false;
        }

        // Mentions specific clubs directly
        if (q.contains("scrs") || q.contains("ieee smc") || q.contains("vishaka") || q.contains("ieee") || q.contains("smc")) {
            return true;
        }

        // Queries inquiring about a specific club (must mention "club" or specific club context)
        if (q.contains("club")) {
            return q.startsWith("tell me about") || q.startsWith("what is") || q.startsWith("details of")
                    || q.startsWith("info on") || q.contains("club details") || q.startsWith("is there a ")
                    || q.contains("about the");
        }

        // Phrases like "tell me about XYZ" where XYZ is not CCMS or general tech
        if (q.startsWith("tell me about ") && !q.contains("ccms") && !q.contains("project") && !q.contains("app") && !q.contains("system")) {
            return true;
        }

        return false;
    }

    private boolean isTechnicalClubsQuery(String q) {
        return q.contains("technical") && !q.contains("non technical") && !q.contains("non-technical") && !q.contains("not technical");
    }

    private boolean isNonTechnicalClubsQuery(String q) {
        return q.contains("non technical") || q.contains("non-technical") || q.contains("cultural");
    }

    private boolean isGeneralClubsListQuery(String q) {
        if (q.contains("how to join") || q.contains("how do i join") || q.contains("my club") || q.contains("i joined") || q.contains("am i a member")) {
            return false;
        }

        return q.contains("what clubs are there")
                || q.contains("what are the clubs")
                || q.contains("what are the clubs here")
                || q.contains("what are the club names")
                || q.contains("give clubs name")
                || q.contains("give club name")
                || q.contains("give me club names")
                || q.contains("give me clubs name")
                || q.contains("list all clubs")
                || q.contains("list clubs")
                || q.contains("which clubs can i join")
                || q.contains("which clubs are there")
                || q.contains("available clubs")
                || q.contains("show clubs")
                || q.contains("show all clubs")
                || q.contains("all clubs")
                || q.contains("club names")
                || q.contains("tell me the clubs")
                || q.contains("what clubs do you have")
                || q.contains("clubs in campus")
                || q.contains("campus clubs")
                || q.equals("clubs")
                || (q.contains("club") && (q.contains("what") || q.contains("list") || q.contains("show") || q.contains("give") || q.contains("all") || q.contains("name") || q.contains("available") || q.contains("here")));
    }

    private boolean isHowToJoinClubQuery(String q) {
        return q.contains("how to join") || q.contains("how do i join") || q.contains("can i join")
                || q.contains("how to become a member") || q.contains("steps to join")
                || (q.contains("join") && (q.contains("club") || q.contains("process") || q.contains("procedure")));
    }

    private boolean isMyClubsQuery(String q) {
        return q.contains("my registered clubs") || q.contains("my clubs") || q.contains("clubs i joined")
                || q.contains("which clubs have i joined") || q.contains("what clubs have i joined")
                || q.contains("what clubs am i a member of") || q.contains("find my registered clubs");
    }

    private boolean isEventsListQuery(String q) {
        if (q.contains("how do i register") || q.contains("how to register") || q.contains("my event") || q.contains("have i registered")) {
            return false;
        }

        return q.contains("what events are available")
                || q.contains("which events are upcoming")
                || q.contains("show upcoming events")
                || q.contains("what events can i register for")
                || q.contains("upcoming events")
                || q.contains("events upcoming")
                || q.contains("what events are coming up")
                || q.contains("what events are there")
                || q.contains("list events")
                || q.contains("list all events")
                || q.contains("list upcoming events")
                || q.contains("show events")
                || q.contains("show all events")
                || q.contains("available events")
                || q.contains("event schedule")
                || q.contains("all events")
                || q.equals("events")
                || (q.contains("event") && (q.contains("what") || q.contains("which") || q.contains("upcoming") || q.contains("list") || q.contains("show") || q.contains("available") || q.contains("schedule") || q.contains("coming")));
    }

    private boolean isHowToRegisterEventQuery(String q) {
        return q.contains("how do i register for an event")
                || q.contains("how to register for an event")
                || q.contains("how to register for events")
                || q.contains("event registration process")
                || q.contains("how can i register")
                || q.contains("how to register")
                || q.contains("how to enroll in an event")
                || q.contains("steps to register for an event")
                || (q.contains("register") && q.contains("event") && (q.contains("how") || q.contains("process") || q.contains("procedure")));
    }

    private boolean isMyRegisteredEventsQuery(String q) {
        return q.contains("which events have i registered for")
                || q.contains("what events have i registered for")
                || q.contains("what events am i registered for")
                || q.contains("show my registered events")
                || q.contains("my registered events")
                || q.contains("my events")
                || q.contains("events i registered for");
    }

    private boolean isMyParticipationQuery(String q) {
        return q.contains("how can i check my participation")
                || q.contains("how do i check my participation")
                || q.contains("how to check my participation")
                || q.contains("how to view participation")
                || q.contains("what is my attendance rate")
                || q.contains("my attendance")
                || q.contains("my participation")
                || q.contains("my attendance percentage")
                || q.contains("check my attendance")
                || q.contains("attendance rate")
                || q.contains("participation rate");
    }

    private boolean isCcmsOverviewQuery(String q) {
        return q.contains("what is ccms") || q.contains("about ccms") || q.contains("purpose of ccms")
                || q.contains("what does ccms do") || q.contains("explain ccms") || q.equals("ccms")
                || (q.contains("ccms") && (q.contains("what") || q.contains("about") || q.contains("purpose") || q.contains("system")));
    }

    private boolean isTechStackQuery(String q) {
        return q.contains("technologies does this project use") || q.contains("tech stack")
                || q.contains("technologies used") || q.contains("what tech does this use")
                || q.contains("tools used") || q.contains("what technologies")
                || q.contains("which technologies") || q.contains("technology stack");
    }

    private boolean isLoginQuery(String q) {
        return q.contains("how does login work") || q.contains("how does authentication work")
                || q.contains("explain login") || q.contains("how to log in")
                || q.contains("authentication process") || q.contains("login system");
    }

    private boolean isJdbcQuery(String q) {
        return q.contains("what is jdbc") || q.contains("how is database connected")
                || q.contains("database connectivity") || q.contains("how does jdbc work")
                || q.contains("explain jdbc");
    }

    private boolean isStorageQuery(String q) {
        return q.contains("how are clubs and events stored") || q.contains("database structure")
                || q.contains("database schema") || q.contains("tables in database")
                || q.contains("what tables exist") || q.contains("how is data stored")
                || q.contains("mysql schema");
    }

    private boolean isChangePasswordQuery(String q) {
        return q.contains("change password") || q.contains("update password") || q.contains("reset password")
                || q.contains("how to change password") || q.contains("how do i change my password")
                || q.equals("password");
    }

    private boolean isChatbotCapabilitiesQuery(String q) {
        return q.contains("what does the chatbot do") || q.contains("what can the chatbot do")
                || q.contains("what can you do") || q.contains("what can you help with")
                || q.contains("how can you help") || q.equals("help") || q.equals("hi")
                || q.equals("hello") || q.equals("who are you") || q.equals("hey");
    }

    // =========================================================================
    // INTENT HANDLERS & DATABASE RETRIEVAL
    // =========================================================================

    private String handlePrivacyViolation() {
        return "🔒 Privacy & Security Notice:\n" +
               "In accordance with CCMS security policies, I can only display your own personal participation, attendance, and registration details.\n" +
               "I cannot disclose records, attendance, or private details for other students.\n\n" +
               "You can view your own records by asking:\n" +
               "• \"Which events have I registered for?\"\n" +
               "• \"How can I check my participation?\"\n" +
               "• or clicking 'My Participation' in the sidebar.";
    }

    private String handleFollowUp(String q) throws SQLException {
        if ("CLUBS".equals(lastTopic) && !lastClubList.isEmpty()) {
            if (q.contains("first")) {
                Club c = lastClubList.get(0);
                return formatClubDetailCard(c);
            }
            if (q.contains("second") && lastClubList.size() >= 2) {
                Club c = lastClubList.get(1);
                return formatClubDetailCard(c);
            }
            if (q.contains("third") && lastClubList.size() >= 3) {
                Club c = lastClubList.get(2);
                return formatClubDetailCard(c);
            }
        }

        if (lastDiscussedClub != null) {
            return formatClubDetailCard(lastDiscussedClub);
        }

        return null; // Let standard classifiers evaluate
    }

    private String handleSpecificClubInquiry(String q, String raw) throws SQLException {
        // Extract club query keyword
        String targetName = extractClubName(q, raw);

        Club matchedClub = clubDAO.findClubByName(targetName);

        if (matchedClub == null) {
            String cleaned = targetName.replaceAll("(?i)\\bclub\\b", "").trim();
            if (!cleaned.isEmpty() && !cleaned.equalsIgnoreCase(targetName)) {
                matchedClub = clubDAO.findClubByName(cleaned);
            }
        }

        if (matchedClub == null) {
            // Also search all clubs with substring contains
            List<Club> all = clubDAO.getAllClubs();
            for (Club c : all) {
                if (q.contains(c.getName().toLowerCase()) || c.getName().toLowerCase().contains(targetName.toLowerCase())) {
                    matchedClub = c;
                    break;
                }
            }
        }

        if (matchedClub != null) {
            lastTopic = "CLUB_DETAIL";
            lastDiscussedClub = matchedClub;
            return formatClubDetailCard(matchedClub);
        }

        // Club not found in the database
        return "The club '" + targetName + "' was not found in the CCMS database.\n" +
               "You can view all registered campus clubs by asking \"What clubs are there?\" or opening the 'Clubs' tab in the left sidebar.";
    }

    private String extractClubName(String q, String raw) {
        if (q.contains("scrs")) return "SCRS";
        if (q.contains("ieee smc") || q.contains("ieee") || q.contains("smc")) return "IEEE SMC";
        if (q.contains("vishaka")) return "Vishaka";

        // Regex patterns to capture target subject after trigger phrase
        Pattern p = Pattern.compile("(?:tell me about|details of|info on|what is|is there a|about)\\s+(?:the\\s+)?([a-zA-Z0-9\\s]+?)(?:\\?|$)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(raw.trim());
        if (m.find()) {
            String extracted = m.group(1).trim();
            if (!extracted.isEmpty() && !extracted.equalsIgnoreCase("the") && !extracted.equalsIgnoreCase("all")) {
                return extracted;
            }
        }

        return raw.replace("?", "").trim();
    }

    private String formatClubDetailCard(Club c) throws SQLException {
        int memberCount = clubDAO.getClubMemberCount(c.getClubId());
        return "🏛 Club Details: " + c.getName() + "\n" +
               "• Category: " + c.getCategory() + "\n" +
               "• Active Members: " + memberCount + " registered\n" +
               "• Overview: " + (c.getDescription() != null ? c.getDescription() : "No description provided.") + "\n\n" +
               "To join " + c.getName() + ", go to the 'Clubs' tab in the sidebar and click 'Join Club'!";
    }

    private String handleClubsList(String categoryFilter) throws SQLException {
        List<Club> clubs;
        if (categoryFilter != null && !categoryFilter.isEmpty()) {
            clubs = clubDAO.getClubsByCategory(categoryFilter);
        } else {
            clubs = clubDAO.getAllClubs();
        }

        if (clubs == null || clubs.isEmpty()) {
            return "No clubs are currently available in the database.";
        }

        lastTopic = "CLUBS";
        lastClubList = clubs;

        StringBuilder sb = new StringBuilder();
        if (categoryFilter != null) {
            sb.append("Here are the ").append(categoryFilter).append(" clubs currently registered in CCMS:\n\n");
        } else {
            sb.append("Here are the campus clubs currently registered in CCMS:\n\n");
        }

        for (int i = 0; i < clubs.size(); i++) {
            Club c = clubs.get(i);
            sb.append(i + 1).append(". ").append(c.getName())
              .append(" (").append(c.getCategory()).append(")\n")
              .append("   ").append(c.getDescription() != null ? c.getDescription() : "No description available.")
              .append("\n\n");
        }

        sb.append("To join any of these clubs, navigate to the 'Clubs' tab in the left sidebar and click 'Join Club'. You can also ask me for details on a specific club (e.g., 'Tell me about SCRS').");
        return sb.toString();
    }

    private String handleHowToJoinClub() throws SQLException {
        List<Club> clubs = clubDAO.getAllClubs();
        StringBuilder sb = new StringBuilder();
        sb.append("To join a campus club in CCMS:\n")
          .append("1. Click the 'Clubs' tab in the left sidebar to open the club catalog.\n")
          .append("2. Browse through the available clubs or filter by Category (Technical / Non-Technical).\n")
          .append("3. Click the 'Join Club' button on any club card.\n")
          .append("4. Your membership is immediately stored in MySQL, and the button will update to 'Leave Club' if you ever wish to cancel.\n\n");

        if (!clubs.isEmpty()) {
            sb.append("Available clubs you can join right now: ");
            List<String> names = new ArrayList<>();
            for (Club c : clubs) names.add(c.getName());
            sb.append(String.join(", ", names)).append(".");
        }

        return sb.toString();
    }

    private String handleMyJoinedClubs(User currentUser) throws SQLException {
        if (currentUser == null) {
            return "Please log in with your student account to view the clubs you have joined.";
        }

        List<Club> joinedClubs = clubDAO.getClubsForUser(currentUser.getUserId());
        if (joinedClubs.isEmpty()) {
            return "You have not joined any campus clubs yet, " + currentUser.getName() + ".\n" +
                   "You can view all available clubs by asking \"What clubs are there?\" or visiting the 'Clubs' tab to join one!";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("You are currently an active member of ").append(joinedClubs.size())
          .append(joinedClubs.size() == 1 ? " club" : " clubs").append(", ")
          .append(currentUser.getName()).append(":\n\n");

        for (int i = 0; i < joinedClubs.size(); i++) {
            Club c = joinedClubs.get(i);
            sb.append(i + 1).append(". ").append(c.getName())
              .append(" (").append(c.getCategory()).append(")\n");
        }

        sb.append("\nYou can discover more clubs anytime in the 'Clubs' tab.");
        return sb.toString();
    }

    private String handleUpcomingEvents() throws SQLException {
        List<SystemEventRecord> events = eventDAO.getUpcomingSystemEvents();
        if (events == null || events.isEmpty()) {
            return "No upcoming events are currently scheduled in the database.";
        }

        lastTopic = "EVENTS";
        lastEventList = events;

        StringBuilder sb = new StringBuilder();
        sb.append("Here are the upcoming campus events currently scheduled in CCMS:\n\n");

        for (int i = 0; i < events.size(); i++) {
            SystemEventRecord e = events.get(i);
            sb.append(i + 1).append(". ").append(e.title).append(" (Organized by ").append(e.clubName).append(")\n")
              .append("   • Date & Time: ").append(e.date).append(" at ").append(e.time).append("\n")
              .append("   • Venue: ").append(e.location).append("\n")
              .append("   • Total Registered: ").append(e.regCount).append(" students\n\n");
        }

        sb.append("To register for an event, go to the 'Events' tab or your Dashboard and click 'Register'.");
        return sb.toString();
    }

    private String handleHowToRegisterEvent() throws SQLException {
        List<SystemEventRecord> events = eventDAO.getUpcomingSystemEvents();
        StringBuilder sb = new StringBuilder();
        sb.append("To register for an event in CCMS:\n")
          .append("1. Open the 'Events' tab in the left sidebar (or view the 'Upcoming Events' section on your Dashboard).\n")
          .append("2. Click the 'Register' button next to the event you wish to attend.\n")
          .append("3. Your registration will be saved immediately to MySQL, and the event will appear under 'My Participation'.\n\n");

        if (events != null && !events.isEmpty()) {
            sb.append("Upcoming events currently open for registration:\n");
            for (SystemEventRecord e : events) {
                sb.append("• ").append(e.title).append(" — ").append(e.date).append(" at ").append(e.time).append(" (").append(e.location).append(")\n");
            }
        } else {
            sb.append("No upcoming events are currently open for registration.");
        }

        return sb.toString();
    }

    private String handleMyRegisteredEvents(User currentUser) throws SQLException {
        if (currentUser == null) {
            return "Please log in with your student account to check the events you have registered for.";
        }

        List<Event> userEvents = eventDAO.getUserRegisteredEvents(currentUser.getUserId());
        if (userEvents.isEmpty()) {
            return "You have not registered for any events yet, " + currentUser.getName() + ".\n" +
                   "You can view open events and register under the 'Events' tab or by asking me 'What events are upcoming?'.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Here are the events you are currently registered for, ").append(currentUser.getName()).append(":\n\n");

        for (int i = 0; i < userEvents.size(); i++) {
            Event e = userEvents.get(i);
            sb.append(i + 1).append(". ").append(e.getTitle()).append("\n")
              .append("   • Date & Time: ").append(e.getEventDate()).append(" at ").append(e.getEventTime()).append("\n")
              .append("   • Venue: ").append(e.getLocation()).append("\n\n");
        }

        sb.append("You can track your complete attendance record in the 'My Participation' tab.");
        return sb.toString();
    }

    private String handleMyParticipation(User currentUser) throws SQLException {
        if (currentUser == null) {
            return "Please log in with your student account to view your participation records.";
        }

        int registeredCount = eventDAO.getUserRegisteredEvents(currentUser.getUserId()).size();
        int attendedCount = eventDAO.getEventsAttendedCount(currentUser.getUserId());
        double rate = eventDAO.getParticipationRate(currentUser.getUserId());

        return "📊 Personal Participation Record for " + currentUser.getName() + ":\n" +
               "• Events Registered: " + registeredCount + "\n" +
               "• Events Attended: " + attendedCount + "\n" +
               "• Overall Participation Rate: " + String.format("%.1f%%", rate) + "\n\n" +
               "Keep actively participating in club activities! You can see your full breakdown under 'My Participation' in the left sidebar.";
    }

    private String handleCcmsOverview() {
        return "CCMS (Campus Club Management System) is a centralized campus platform designed to connect students with campus clubs, discover upcoming club events, register for activities, and track event participation and attendance across university clubs.";
    }

    private String handleTechStack() {
        return "CCMS is built using a modern desktop architecture:\n" +
               "• Language: Java 17 (LTS)\n" +
               "• GUI Framework: JavaFX 21 (Modular Desktop UI with Custom Glassmorphism CSS)\n" +
               "• Build Tool: Apache Maven\n" +
               "• Database: MySQL with JDBC and PreparedStatements\n" +
               "• Security: BCrypt Password Hashing ($2a$10$...) via jBCrypt\n" +
               "• Architecture: Layered MVC Pattern (Model, View, Controller, DAO, Service)";
    }

    private String handleLoginExplanation() {
        return "Login in CCMS works through role-based authentication (Student, Coordinator, Admin):\n" +
               "1. The user inputs their Email/Roll number and password and selects their role.\n" +
               "2. LoginController calls UserDAO, which executes a parameterized PreparedStatement query.\n" +
               "3. PasswordUtil securely verifies the entered password against the BCrypt hash stored in MySQL.\n" +
               "4. If valid, the system opens the corresponding role-based dashboard.";
    }

    private String handleJdbcExplanation() {
        return "JDBC (Java Database Connectivity) is the standard Java API that connects CCMS to the MySQL database. " +
               "In this project, 'DBConnection.java' manages connections, externalizes credentials cleanly via db.properties or environment variables, " +
               "and ensures all DAO operations use PreparedStatements to prevent SQL injection.";
    }

    private String handleStorageExplanation() {
        return "CCMS stores all data in a relational MySQL schema with 6 core tables:\n" +
               "1. 'users': Stores user accounts with roles (Student, Coordinator, Admin) and BCrypt passwords.\n" +
               "2. 'clubs': Stores campus clubs, categories, descriptions, and assigned coordinators.\n" +
               "3. 'club_members': Records student club memberships and join dates.\n" +
               "4. 'events': Stores scheduled events with dates, times, venues, and status.\n" +
               "5. 'event_registrations': Tracks student event enrollments.\n" +
               "6. 'attendance': Records verified event attendance marked by club coordinators.";
    }

    private String handleChangePasswordExplanation() {
        return "To change your password:\n" +
               "1. Click the '🔑 Change Password' button located at the bottom of the left sidebar.\n" +
               "2. Enter your current password and your new password.\n" +
               "3. When you submit, ChangePasswordDialog validates your credentials and UserDAO updates your password in MySQL using BCrypt hashing.";
    }

    private String handleChatbotCapabilities() {
        return "I am the CCMS Campus Assistant! I am connected to the live database and can answer:\n" +
               "• Live Clubs: \"What clubs are there?\", \"Give clubs name\", \"Show technical clubs\"\n" +
               "• Club Details: \"Tell me about SCRS\", \"What is IEEE SMC?\"\n" +
               "• Live Events: \"What events are upcoming?\", \"What events can I register for?\"\n" +
               "• Event Registration: \"How do I register for an event?\"\n" +
               "• Your Participation: \"Which events have I registered for?\", \"How can I check my participation?\"\n" +
               "• Your Clubs: \"Which clubs have I joined?\"\n" +
               "• Project Details: \"What is CCMS?\", \"Which technologies does this project use?\", \"How does login work?\"\n" +
               "• Account: \"How do I change my password?\"";
    }

    private String handleFallback(String raw) {
        return "I don't have information about that question. As the CCMS Assistant, I can answer questions about campus clubs, upcoming events, registrations, your attendance, and CCMS project features.\n\n" +
               "Try asking:\n" +
               "• \"What clubs are there?\"\n" +
               "• \"What events are upcoming?\"\n" +
               "• \"Which events have I registered for?\"\n" +
               "• \"Which technologies does this project use?\"";
    }
}
