package com.campusclub.service;

import com.campusclub.dao.ClubDAO;
import com.campusclub.dao.EventDAO;
import com.campusclub.dao.EventDAO.SystemEventRecord;
import com.campusclub.model.Club;
import com.campusclub.model.Event;
import com.campusclub.model.User;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Automated Verification Harness testing the 10 required Chatbot scenarios.
 * Uses test DAO stubs matching CCMS seed data to verify intent parsing,
 * database grounding, privacy guards, follow-ups, and empty query fallbacks.
 */
public class ChatbotServiceTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  CCMS CHATBOT SERVICE 10-POINT TEST HARNESS    ");
        System.out.println("=================================================\n");

        // Set up test DAOs populated with the actual CCMS database entities
        TestClubDAO testClubDAO = new TestClubDAO();
        TestEventDAO testEventDAO = new TestEventDAO();

        ChatbotService bot = new ChatbotService(testClubDAO, testEventDAO);
        User loggedInStudent = new User(2, "Charmi", "99250040558@klu.ac.in", "", "Student");

        int passed = 0;
        int total = 10;

        // Case 1: "What clubs are there?"
        System.out.println("[Test 1] User asks: 'What clubs are there?'");
        String r1 = bot.getResponse("What clubs are there?", loggedInStudent);
        System.out.println(r1);
        boolean p1 = r1.contains("SCRS") && r1.contains("IEEE SMC") && r1.contains("Vishaka");
        printResult(1, p1);
        if (p1) passed++;

        // Case 2: "Give clubs name."
        System.out.println("\n[Test 2] User asks: 'Give clubs name.'");
        String r2 = bot.getResponse("Give clubs name.", loggedInStudent);
        System.out.println(r2);
        boolean p2 = r2.contains("SCRS") && r2.contains("IEEE SMC") && r2.contains("Vishaka");
        printResult(2, p2);
        if (p2) passed++;

        // Case 3: "List all technical clubs."
        System.out.println("\n[Test 3] User asks: 'List all technical clubs.'");
        String r3 = bot.getResponse("List all technical clubs.", loggedInStudent);
        System.out.println(r3);
        boolean p3 = r3.contains("SCRS") && r3.contains("IEEE SMC") && !r3.contains("Vishaka (Non-Technical)");
        printResult(3, p3);
        if (p3) passed++;

        // Case 4: "What events are upcoming?"
        System.out.println("\n[Test 4] User asks: 'What events are upcoming?'");
        String r4 = bot.getResponse("What events are upcoming?", loggedInStudent);
        System.out.println(r4);
        boolean p4 = r4.contains("Soft Computing & AI Workshop") && r4.contains("Autonomous Systems Hackathon");
        printResult(4, p4);
        if (p4) passed++;

        // Case 5: A question about a club that actually exists ("Tell me about SCRS")
        System.out.println("\n[Test 5] User asks: 'Tell me about SCRS'");
        String r5 = bot.getResponse("Tell me about SCRS", loggedInStudent);
        System.out.println(r5);
        boolean p5 = r5.contains("Club Details: SCRS") && r5.contains("Technical") && r5.contains("advanced computing domains");
        printResult(5, p5);
        if (p5) passed++;

        // Case 6: A question about a club that does NOT exist ("Tell me about Robotics Club")
        System.out.println("\n[Test 6] User asks: 'Tell me about Robotics Club'");
        String r6 = bot.getResponse("Tell me about Robotics Club", loggedInStudent);
        System.out.println(r6);
        boolean p6 = r6.contains("The club 'Robotics Club' was not found in the CCMS database");
        printResult(6, p6);
        if (p6) passed++;

        // Case 7: Database query that returns no results (empty database)
        System.out.println("\n[Test 7] Database query with empty result set");
        ChatbotService emptyBot = new ChatbotService(new EmptyClubDAO(), new EmptyEventDAO());
        String r7 = emptyBot.getResponse("What clubs are there?", loggedInStudent);
        System.out.println(r7);
        boolean p7 = r7.equals("No clubs are currently available in the database.");
        printResult(7, p7);
        if (p7) passed++;

        // Case 8: A question completely unrelated to CCMS ("What is the weather today?")
        System.out.println("\n[Test 8] User asks unrelated question: 'What is the weather today?'");
        String r8 = bot.getResponse("What is the weather today in Vijayawada?", loggedInStudent);
        System.out.println(r8);
        boolean p8 = r8.contains("I don't have information about that question") && r8.contains("CCMS Assistant");
        printResult(8, p8);
        if (p8) passed++;

        // Case 9: A follow-up question about a previous answer
        System.out.println("\n[Test 9] Follow-up question: 'Tell me more about the first one'");
        // Context established by Test 1 ("What clubs are there?"): first club is SCRS
        bot.getResponse("What clubs are there?", loggedInStudent);
        String r9 = bot.getResponse("Tell me more about the first one", loggedInStudent);
        System.out.println(r9);
        boolean p9 = r9.contains("Club Details: SCRS") && r9.contains("Technical");
        printResult(9, p9);
        if (p9) passed++;

        // Case 10: Student asking for another student's private participation details ("Show NEHA's events")
        System.out.println("\n[Test 10] Privacy attempt: 'Show NEHA's events' (asked by Charmi)");
        String r10 = bot.getResponse("Show NEHA's events and attendance", loggedInStudent);
        System.out.println(r10);
        boolean p10 = r10.contains("Privacy & Security Notice") && r10.contains("cannot disclose records");
        printResult(10, p10);
        if (p10) passed++;

        System.out.println("\n=================================================");
        System.out.println("  SUMMARY: " + passed + " / " + total + " TESTS PASSED");
        System.out.println("=================================================");
    }

    private static void printResult(int testNum, boolean passed) {
        System.out.println(">>> Test " + testNum + " " + (passed ? "PASSED ✅" : "FAILED ❌"));
    }

    // =========================================================================
    // TEST DAOs (Mirroring actual CCMS MySQL DB records for automated testing)
    // =========================================================================

    private static class TestClubDAO extends ClubDAO {
        private final List<Club> clubs = new ArrayList<>();

        public TestClubDAO() {
            clubs.add(new Club(1, "SCRS", "Technical", "Encouraging undergraduate and postgraduate students to explore advanced computing domains and publish scientific papers.", "⚙️", "Active"));
            clubs.add(new Club(2, "IEEE SMC", "Technical", "Brainstorming and building real-world application prototypes during collaborative campus marathons.", "🤖", "Not Joined"));
            clubs.add(new Club(4, "Vishaka", "Non-Technical", "The club acts as a creative hub dedicated to blending cultural tradition with innovative entertainment, fostering artistic expression among university students.", "🌸", "Not Joined"));
        }

        @Override
        public List<Club> getAllClubs() {
            return new ArrayList<>(clubs);
        }

        @Override
        public List<Club> getClubsByCategory(String category) {
            List<Club> filtered = new ArrayList<>();
            for (Club c : clubs) {
                if (c.getCategory().equalsIgnoreCase(category)) {
                    filtered.add(c);
                }
            }
            return filtered;
        }

        @Override
        public Club findClubByName(String name) {
            for (Club c : clubs) {
                if (c.getName().equalsIgnoreCase(name) || c.getName().toLowerCase().contains(name.toLowerCase())) {
                    return c;
                }
            }
            return null;
        }

        @Override
        public int getClubMemberCount(int clubId) {
            return 12; // Sample active member count
        }

        @Override
        public List<Club> getClubsForUser(int userId) {
            List<Club> joined = new ArrayList<>();
            joined.add(clubs.get(0)); // SCRS
            return joined;
        }
    }

    private static class TestEventDAO extends EventDAO {
        private final List<SystemEventRecord> events = new ArrayList<>();

        public TestEventDAO() {
            events.add(new SystemEventRecord(1, "SCRS", "Soft Computing & AI Workshop", "Nov 15, 2026", "10:00 AM", "Room 301, Engineering Block", 15));
            events.add(new SystemEventRecord(2, "IEEE SMC", "Autonomous Systems Hackathon", "Nov 20, 2026", "09:00 AM", "Innovation Lab 2", 24));
            events.add(new SystemEventRecord(3, "Vishaka", "Vishaka Cultural Orientation", "Nov 28, 2026", "03:00 PM", "Main Auditorium", 40));
        }

        @Override
        public List<SystemEventRecord> getUpcomingSystemEvents() {
            return new ArrayList<>(events);
        }

        @Override
        public List<Event> getUserRegisteredEvents(int userId) {
            List<Event> userEvts = new ArrayList<>();
            userEvts.add(new Event(1, 1, "Soft Computing & AI Workshop", "Nov 15, 2026", "10:00 AM", "Room 301, Engineering Block", "Workshop", true));
            return userEvts;
        }

        @Override
        public int getEventsAttendedCount(int userId) {
            return 1;
        }

        @Override
        public double getParticipationRate(int userId) {
            return 100.0;
        }
    }

    private static class EmptyClubDAO extends ClubDAO {
        @Override
        public List<Club> getAllClubs() {
            return Collections.emptyList();
        }

        @Override
        public List<Club> getClubsByCategory(String category) {
            return Collections.emptyList();
        }

        @Override
        public Club findClubByName(String name) {
            return null;
        }
    }

    private static class EmptyEventDAO extends EventDAO {
        @Override
        public List<SystemEventRecord> getUpcomingSystemEvents() {
            return Collections.emptyList();
        }

        @Override
        public List<Event> getUserRegisteredEvents(int userId) {
            return Collections.emptyList();
        }
    }
}
