package com.campusclub.view;

import com.campusclub.dao.ClubDAO;
import com.campusclub.dao.EventDAO;
import com.campusclub.dao.EventDAO.StudentParticipation;
import com.campusclub.dao.EventDAO.StudentRegistration;
import com.campusclub.dao.UserDAO;
import com.campusclub.model.Club;
import com.campusclub.model.Event;
import com.campusclub.model.User;
import com.campusclub.util.AlertUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Coordinator Dashboard View for managing assigned club activities, events, registrations, attendance, and participation.
 */
public class CoordinatorDashboardView extends BorderPane {

    private final User coordinatorUser;
    private final Runnable onLogout;

    private final ClubDAO clubDAO;
    private final EventDAO eventDAO;
    private final UserDAO userDAO;

    private Club assignedClub;
    private VBox mainContentArea;

    // Sidebar navigation buttons
    private Button btnDashboard;
    private Button btnManageEvents;
    private Button btnRegistrations;
    private Button btnAttendance;
    private Button btnParticipation;

    public CoordinatorDashboardView(User user, Runnable onLogout) {
        this.coordinatorUser = user;
        this.onLogout = onLogout;
        this.clubDAO = new ClubDAO();
        this.eventDAO = new EventDAO();
        this.userDAO = new UserDAO();

        loadAssignedClub();
        initializeUI();
    }

    private void loadAssignedClub() {
        try {
            this.assignedClub = clubDAO.getClubForCoordinator(coordinatorUser.getUserId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initializeUI() {
        this.getStyleClass().add("dashboard-root");

        // 1. Sidebar Navigation
        VBox sidebar = createSidebar();
        this.setLeft(sidebar);

        // 2. Main Scrollable Content Area
        mainContentArea = new VBox(24);
        mainContentArea.setPadding(new Insets(30, 35, 35, 35));
        mainContentArea.setStyle("-fx-background-color: #F8FAFC;");

        ScrollPane scrollPane = new ScrollPane(mainContentArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: #F8FAFC;");

        this.setCenter(scrollPane);

        // Default view: Dashboard Overview
        showDashboardView();
    }

    /**
     * Builds dark navigation sidebar matching CCMS design specs.
     */
    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(250);
        sidebar.setPadding(new Insets(25, 20, 25, 20));
        sidebar.setSpacing(12);
        sidebar.setStyle("-fx-background-color: #181C2E;");

        // CCMS Header
        Label brandLabel = new Label("🎓 CCMS");
        brandLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        brandLabel.setTextFill(Color.WHITE);

        Label brandSubtitle = new Label("Coordinator Portal");
        brandSubtitle.setFont(Font.font("System", FontWeight.NORMAL, 12));
        brandSubtitle.setTextFill(Color.web("#94A3B8"));

        VBox brandBox = new VBox(4, brandLabel, brandSubtitle);
        brandBox.setPadding(new Insets(0, 0, 15, 5));

        // Navigation Buttons
        btnDashboard = createNavButton("📊 Dashboard");
        btnManageEvents = createNavButton("📅 Manage Events");
        btnRegistrations = createNavButton("📋 Event Registrations");
        btnAttendance = createNavButton("✅ Attendance");
        btnParticipation = createNavButton("📈 Participation");

        btnDashboard.setOnAction(e -> { selectNav(btnDashboard); showDashboardView(); });
        btnManageEvents.setOnAction(e -> { selectNav(btnManageEvents); showManageEventsView(); });
        btnRegistrations.setOnAction(e -> { selectNav(btnRegistrations); showRegistrationsView(); });
        btnAttendance.setOnAction(e -> { selectNav(btnAttendance); showAttendanceView(); });
        btnParticipation.setOnAction(e -> { selectNav(btnParticipation); showParticipationView(); });

        selectNav(btnDashboard);

        VBox navBox = new VBox(8, btnDashboard, btnManageEvents, btnRegistrations, btnAttendance, btnParticipation);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Sidebar Footer - STRICT ORDER REQUIRED:
        // Logged-in Coordinator Name -> Coordinator -> Change Password -> Logout
        Label nameLabel = new Label(coordinatorUser != null ? coordinatorUser.getName() : "Coordinator");
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 15));
        nameLabel.setTextFill(Color.WHITE);

        Label roleBadge = new Label("Coordinator");
        roleBadge.setFont(Font.font("System", FontWeight.NORMAL, 12));
        roleBadge.setTextFill(Color.web("#818CF8"));

        VBox userDetailsBox = new VBox(2, nameLabel, roleBadge);
        userDetailsBox.setPadding(new Insets(10, 5, 10, 5));

        Button btnChangePassword = new Button("🔑 Change Password");
        btnChangePassword.setMaxWidth(Double.MAX_VALUE);
        btnChangePassword.setAlignment(Pos.CENTER_LEFT);
        btnChangePassword.setStyle("-fx-background-color: transparent; -fx-text-fill: #CBD5E1; -fx-cursor: hand; -fx-font-size: 13px;");
        btnChangePassword.setOnAction(e -> openChangePasswordModal());

        Button btnLogout = new Button("🚪 Logout");
        btnLogout.setMaxWidth(Double.MAX_VALUE);
        btnLogout.setAlignment(Pos.CENTER_LEFT);
        btnLogout.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 8 12;");
        btnLogout.setOnAction(e -> {
            if (onLogout != null) {
                onLogout.run();
            }
        });

        VBox footerBox = new VBox(8, userDetailsBox, btnChangePassword, btnLogout);
        footerBox.setStyle("-fx-border-color: #2D3748; -fx-border-width: 1 0 0 0; -fx-padding: 15 0 0 0;");

        sidebar.getChildren().addAll(brandBox, navBox, spacer, footerBox);
        return sidebar;
    }

    private Button createNavButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 10 14; -fx-background-radius: 8px;");
        return btn;
    }

    private void selectNav(Button selected) {
        Button[] navs = {btnDashboard, btnManageEvents, btnRegistrations, btnAttendance, btnParticipation};
        for (Button b : navs) {
            b.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 10 14; -fx-background-radius: 8px;");
        }
        selected.setStyle("-fx-background-color: #6366F1; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10 14; -fx-background-radius: 8px;");
    }

    // =========================================================================
    // 1. MAIN DASHBOARD OVERVIEW
    // =========================================================================
    private void showDashboardView() {
        mainContentArea.getChildren().clear();

        // Welcome Header
        Label welcomeTitle = new Label("Welcome, " + coordinatorUser.getName() + "!");
        welcomeTitle.setFont(Font.font("System", FontWeight.BOLD, 26));
        welcomeTitle.setTextFill(Color.web("#1E293B"));

        Label welcomeSub = new Label("Here's an overview of your club activities.");
        welcomeSub.setFont(Font.font("System", FontWeight.NORMAL, 14));
        welcomeSub.setTextFill(Color.web("#64748B"));

        VBox header = new VBox(4, welcomeTitle, welcomeSub);

        // Fetch Live Statistic Counts from DB
        int totalEvents = 0;
        int totalRegs = 0;
        int totalAtt = 0;
        String clubName = assignedClub != null ? assignedClub.getName() : "No Assigned Club";

        if (assignedClub != null) {
            try {
                totalEvents = eventDAO.getEventsCountForClub(assignedClub.getClubId());
                totalRegs = eventDAO.getRegistrationsCountForClub(assignedClub.getClubId());
                totalAtt = eventDAO.getAttendanceCountForClub(assignedClub.getClubId());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // Statistic Cards Grid
        HBox statsGrid = new HBox(16);
        statsGrid.getChildren().addAll(
            createStatCard("🏛 My Club", clubName, "#EEF2FF", "#4F46E5"),
            createStatCard("📅 Total Events", String.valueOf(totalEvents), "#F0FDF4", "#16A34A"),
            createStatCard("📋 Total Registrations", String.valueOf(totalRegs), "#FEF3C7", "#D97706"),
            createStatCard("✅ Attendance", String.valueOf(totalAtt), "#F3E8FF", "#9333EA")
        );

        // My Club Details Section
        VBox clubSection = createMyClubSection();

        // Upcoming Events Section
        VBox eventsSection = createUpcomingEventsSection();

        mainContentArea.getChildren().addAll(header, statsGrid, clubSection, eventsSection);
    }

    private VBox createStatCard(String title, String value, String bgColor, String accentColor) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(18, 20, 18, 20));
        card.setPrefWidth(210);
        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 12px; -fx-border-color: " + accentColor + "33; -fx-border-radius: 12px;");

        Label titleLbl = new Label(title);
        titleLbl.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        titleLbl.setTextFill(Color.web("#475569"));

        Label valLbl = new Label(value);
        valLbl.setFont(Font.font("System", FontWeight.BOLD, 22));
        valLbl.setTextFill(Color.web(accentColor));

        card.getChildren().addAll(titleLbl, valLbl);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox createMyClubSection() {
        VBox section = new VBox(12);
        section.setPadding(new Insets(20));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 12px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 8, 0, 0, 2);");

        Label sectionTitle = new Label("🏛 My Assigned Club");
        sectionTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        sectionTitle.setTextFill(Color.web("#1E293B"));

        if (assignedClub == null) {
            Label noClubLbl = new Label("No club is currently assigned to your coordinator account.");
            noClubLbl.setTextFill(Color.web("#64748B"));
            section.getChildren().addAll(sectionTitle, noClubLbl);
            return section;
        }

        Label nameLbl = new Label(assignedClub.getName() + " (" + assignedClub.getCategory() + ")");
        nameLbl.setFont(Font.font("System", FontWeight.BOLD, 16));
        nameLbl.setTextFill(Color.web("#4F46E5"));

        Label descLbl = new Label(assignedClub.getDescription());
        descLbl.setFont(Font.font("System", FontWeight.NORMAL, 13));
        descLbl.setTextFill(Color.web("#475569"));
        descLbl.setWrapText(true);

        Label coordLbl = new Label("Coordinator: " + coordinatorUser.getName() + " (" + coordinatorUser.getEmail() + ")");
        coordLbl.setFont(Font.font("System", FontWeight.SEMI_BOLD, 12));
        coordLbl.setTextFill(Color.web("#64748B"));

        section.getChildren().addAll(sectionTitle, nameLbl, descLbl, coordLbl);
        return section;
    }

    private VBox createUpcomingEventsSection() {
        VBox section = new VBox(12);
        section.setPadding(new Insets(20));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 12px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 8, 0, 0, 2);");

        Label sectionTitle = new Label("📅 Club Upcoming Events");
        sectionTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        sectionTitle.setTextFill(Color.web("#1E293B"));

        List<Event> events = new ArrayList<>();
        if (assignedClub != null) {
            try {
                events = eventDAO.getEventsByClub(assignedClub.getClubId());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if (events.isEmpty()) {
            Label emptyLbl = new Label("No upcoming events");
            emptyLbl.setFont(Font.font("System", FontWeight.MEDIUM, 14));
            emptyLbl.setTextFill(Color.web("#94A3B8"));
            emptyLbl.setPadding(new Insets(10, 0, 10, 0));
            section.getChildren().addAll(sectionTitle, emptyLbl);
            return section;
        }

        VBox eventsList = new VBox(10);
        for (Event event : events) {
            HBox item = new HBox(15);
            item.setAlignment(Pos.CENTER_LEFT);
            item.setPadding(new Insets(12, 16, 12, 16));
            item.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 8px; -fx-border-color: #E2E8F0; -fx-border-radius: 8px;");

            Label title = new Label(event.getTitle());
            title.setFont(Font.font("System", FontWeight.BOLD, 14));
            title.setPrefWidth(220);

            Label date = new Label("📅 " + event.getEventDate() + " " + event.getEventTime());
            date.setFont(Font.font("System", FontWeight.NORMAL, 13));
            date.setTextFill(Color.web("#64748B"));
            date.setPrefWidth(180);

            Label venue = new Label("📍 " + event.getLocation());
            venue.setFont(Font.font("System", FontWeight.NORMAL, 13));
            venue.setTextFill(Color.web("#64748B"));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label status = new Label("Upcoming");
            status.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-padding: 4 10; -fx-background-radius: 12px; -fx-font-weight: bold; -fx-font-size: 11px;");

            item.getChildren().addAll(title, date, venue, spacer, status);
            eventsList.getChildren().add(item);
        }

        section.getChildren().addAll(sectionTitle, eventsList);
        return section;
    }

    // =========================================================================
    // 2. MANAGE EVENTS VIEW (CRUD)
    // =========================================================================
    private void showManageEventsView() {
        mainContentArea.getChildren().clear();

        Label title = new Label("📅 Manage Events");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#1E293B"));

        Label subtitle = new Label("Create, edit, or remove events for your assigned club.");
        subtitle.setFont(Font.font("System", FontWeight.NORMAL, 14));
        subtitle.setTextFill(Color.web("#64748B"));

        Button btnCreate = new Button("➕ Create New Event");
        btnCreate.setStyle("-fx-background-color: #4F46E5; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 8px; -fx-cursor: hand;");
        btnCreate.setOnAction(e -> openEventModal(null));

        HBox topBox = new HBox(new VBox(4, title, subtitle));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topBox.getChildren().addAll(spacer, btnCreate);
        topBox.setAlignment(Pos.CENTER_LEFT);

        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: white; -fx-background-radius: 12px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 8, 0, 0, 2);");

        List<Event> events = new ArrayList<>();
        if (assignedClub != null) {
            try {
                events = eventDAO.getEventsByClub(assignedClub.getClubId());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if (events.isEmpty()) {
            Label empty = new Label("No events created yet. Click 'Create New Event' above to add your first event.");
            empty.setTextFill(Color.web("#64748B"));
            empty.setPadding(new Insets(20, 0, 20, 0));
            container.getChildren().add(empty);
        } else {
            for (Event event : events) {
                HBox card = new HBox(15);
                card.setAlignment(Pos.CENTER_LEFT);
                card.setPadding(new Insets(14, 18, 14, 18));
                card.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 8px; -fx-border-color: #E2E8F0; -fx-border-radius: 8px;");

                VBox details = new VBox(4);
                Label evTitle = new Label(event.getTitle());
                evTitle.setFont(Font.font("System", FontWeight.BOLD, 15));

                Label evMeta = new Label("📅 " + event.getEventDate() + " at " + event.getEventTime() + " | 📍 " + event.getLocation());
                evMeta.setFont(Font.font("System", FontWeight.NORMAL, 13));
                evMeta.setTextFill(Color.web("#64748B"));

                Label evDesc = new Label(event.getDescription());
                evDesc.setFont(Font.font("System", FontWeight.NORMAL, 12));
                evDesc.setTextFill(Color.web("#475569"));

                details.getChildren().addAll(evTitle, evMeta, evDesc);
                HBox.setHgrow(details, Priority.ALWAYS);

                Button btnEdit = new Button("✏️ Edit");
                btnEdit.setStyle("-fx-background-color: #E0E7FF; -fx-text-fill: #4338CA; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6px;");
                btnEdit.setOnAction(e -> openEventModal(event));

                Button btnDelete = new Button("🗑 Delete");
                btnDelete.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6px;");
                btnDelete.setOnAction(e -> {
                    if (AlertUtils.showConfirmation("Confirm Delete", "Delete Event", "Are you sure you want to delete '" + event.getTitle() + "'?")) {
                        try {
                            eventDAO.deleteEvent(event.getEventId());
                            AlertUtils.showInfo("Success", "Event Deleted", "The event has been deleted successfully.");
                            showManageEventsView();
                        } catch (SQLException ex) {
                            AlertUtils.showError("Database Error", "Delete Failed", ex.getMessage());
                        }
                    }
                });

                HBox actions = new HBox(8, btnEdit, btnDelete);
                actions.setAlignment(Pos.CENTER);

                card.getChildren().addAll(details, actions);
                container.getChildren().add(card);
            }
        }

        mainContentArea.getChildren().addAll(topBox, container);
    }

    private void openEventModal(Event existingEvent) {
        if (assignedClub == null) {
            AlertUtils.showWarning("Warning", "No Assigned Club", "You cannot manage events without an assigned club.");
            return;
        }

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(existingEvent == null ? "Create New Event" : "Edit Event");

        VBox form = new VBox(14);
        form.setPadding(new Insets(24));
        form.setStyle("-fx-background-color: white;");

        TextField titleFld = new TextField(existingEvent != null ? existingEvent.getTitle() : "");
        titleFld.setPromptText("e.g. Annual Tech Symposium");

        TextField dateFld = new TextField(existingEvent != null ? existingEvent.getEventDate() : "");
        dateFld.setPromptText("e.g. Oct 25, 2026");

        TextField timeFld = new TextField(existingEvent != null ? existingEvent.getEventTime() : "");
        timeFld.setPromptText("e.g. 10:00 AM");

        TextField locFld = new TextField(existingEvent != null ? existingEvent.getLocation() : "");
        locFld.setPromptText("e.g. Main Auditorium");

        TextArea descFld = new TextArea(existingEvent != null ? existingEvent.getDescription() : "");
        descFld.setPromptText("Enter event description");
        descFld.setPrefRowCount(3);

        Button btnSave = new Button(existingEvent == null ? "Save Event" : "Update Event");
        btnSave.setStyle("-fx-background-color: #4F46E5; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6px; -fx-cursor: hand;");
        btnSave.setOnAction(e -> {
            String title = titleFld.getText().trim();
            String date = dateFld.getText().trim();
            String time = timeFld.getText().trim();
            String loc = locFld.getText().trim();
            String desc = descFld.getText().trim();

            if (title.isEmpty() || date.isEmpty() || time.isEmpty() || loc.isEmpty()) {
                AlertUtils.showWarning("Validation Error", "Missing Fields", "Please fill in Event Title, Date, Time, and Venue.");
                return;
            }

            try {
                if (existingEvent == null) {
                    eventDAO.createEvent(assignedClub.getClubId(), title, date, time, loc, desc);
                    AlertUtils.showInfo("Success", "Event Created", "New event created successfully!");
                } else {
                    eventDAO.updateEvent(existingEvent.getEventId(), title, date, time, loc, desc);
                    AlertUtils.showInfo("Success", "Event Updated", "Event updated successfully!");
                }
                dialog.close();
                showManageEventsView();
            } catch (SQLException ex) {
                AlertUtils.showError("Database Error", "Save Failed", ex.getMessage());
            }
        });

        form.getChildren().addAll(
            new Label("Event Title:"), titleFld,
            new Label("Event Date:"), dateFld,
            new Label("Event Time:"), timeFld,
            new Label("Venue / Location:"), locFld,
            new Label("Description:"), descFld,
            btnSave
        );

        Scene scene = new Scene(form, 420, 480);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // =========================================================================
    // 3. EVENT REGISTRATIONS VIEW
    // =========================================================================
    private void showRegistrationsView() {
        mainContentArea.getChildren().clear();

        Label title = new Label("📋 Event Registrations");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));

        Label subtitle = new Label("Select an event to view registered students.");
        subtitle.setFont(Font.font("System", FontWeight.NORMAL, 14));
        subtitle.setTextFill(Color.web("#64748B"));

        VBox top = new VBox(4, title, subtitle);

        ComboBox<Event> eventCombo = new ComboBox<>();
        eventCombo.setPrefWidth(350);

        List<Event> events = new ArrayList<>();
        if (assignedClub != null) {
            try {
                events = eventDAO.getEventsByClub(assignedClub.getClubId());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        eventCombo.setItems(FXCollections.observableArrayList(events));

        VBox regListContainer = new VBox(12);

        eventCombo.setOnAction(e -> {
            Event selected = eventCombo.getValue();
            if (selected != null) {
                populateRegistrations(regListContainer, selected);
            }
        });

        if (!events.isEmpty()) {
            eventCombo.setValue(events.get(0));
            populateRegistrations(regListContainer, events.get(0));
        } else {
            regListContainer.getChildren().add(new Label("No events available. Create an event first."));
        }

        HBox selectBox = new HBox(10, new Label("Select Event:"), eventCombo);
        selectBox.setAlignment(Pos.CENTER_LEFT);

        mainContentArea.getChildren().addAll(top, selectBox, regListContainer);
    }

    private void populateRegistrations(VBox container, Event event) {
        container.getChildren().clear();

        try {
            List<StudentRegistration> regs = eventDAO.getRegistrationsForEvent(event.getEventId());

            if (regs.isEmpty()) {
                Label empty = new Label("No students registered for '" + event.getTitle() + "' yet.");
                empty.setStyle("-fx-text-fill: #64748B; -fx-padding: 15;");
                container.getChildren().add(empty);
                return;
            }

            TableView<StudentRegistration> table = new TableView<>();
            table.setPrefHeight(300);

            TableColumn<StudentRegistration, String> nameCol = new TableColumn<>("Student Name");
            nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name));
            nameCol.setPrefWidth(180);

            TableColumn<StudentRegistration, String> emailCol = new TableColumn<>("Email");
            emailCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().email));
            emailCol.setPrefWidth(220);

            TableColumn<StudentRegistration, String> dateCol = new TableColumn<>("Registration Date");
            dateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().registeredAt));
            dateCol.setPrefWidth(180);

            TableColumn<StudentRegistration, String> statusCol = new TableColumn<>("Status");
            statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status));
            statusCol.setPrefWidth(120);

            table.getColumns().addAll(nameCol, emailCol, dateCol, statusCol);
            table.setItems(FXCollections.observableArrayList(regs));

            container.getChildren().add(table);
        } catch (SQLException e) {
            AlertUtils.showError("Error", "Failed to fetch registrations", e.getMessage());
        }
    }

    // =========================================================================
    // 4. ATTENDANCE VIEW
    // =========================================================================
    private void showAttendanceView() {
        mainContentArea.getChildren().clear();

        Label title = new Label("✅ Mark Event Attendance");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));

        Label subtitle = new Label("Select an event and mark registered students as Present or Absent.");
        subtitle.setFont(Font.font("System", FontWeight.NORMAL, 14));
        subtitle.setTextFill(Color.web("#64748B"));

        VBox top = new VBox(4, title, subtitle);

        ComboBox<Event> eventCombo = new ComboBox<>();
        eventCombo.setPrefWidth(350);

        List<Event> events = new ArrayList<>();
        if (assignedClub != null) {
            try {
                events = eventDAO.getEventsByClub(assignedClub.getClubId());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        eventCombo.setItems(FXCollections.observableArrayList(events));

        VBox attContent = new VBox(15);

        eventCombo.setOnAction(e -> {
            Event sel = eventCombo.getValue();
            if (sel != null) {
                populateAttendanceForm(attContent, sel);
            }
        });

        if (!events.isEmpty()) {
            eventCombo.setValue(events.get(0));
            populateAttendanceForm(attContent, events.get(0));
        } else {
            attContent.getChildren().add(new Label("No events available for marking attendance."));
        }

        HBox selectBox = new HBox(10, new Label("Select Event:"), eventCombo);
        selectBox.setAlignment(Pos.CENTER_LEFT);

        mainContentArea.getChildren().addAll(top, selectBox, attContent);
    }

    private void populateAttendanceForm(VBox container, Event event) {
        container.getChildren().clear();

        try {
            List<StudentRegistration> regs = eventDAO.getRegistrationsForEvent(event.getEventId());

            if (regs.isEmpty()) {
                Label empty = new Label("No registered students found for '" + event.getTitle() + "'.");
                empty.setStyle("-fx-text-fill: #64748B; -fx-padding: 15;");
                container.getChildren().add(empty);
                return;
            }

            VBox rowsBox = new VBox(10);
            rowsBox.setPadding(new Insets(15));
            rowsBox.setStyle("-fx-background-color: white; -fx-background-radius: 10px;");

            List<ToggleGroup> groups = new ArrayList<>();

            for (StudentRegistration reg : regs) {
                HBox row = new HBox(15);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8, 12, 8, 12));
                row.setStyle("-fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");

                Label sName = new Label(reg.name + " (" + reg.email + ")");
                sName.setFont(Font.font("System", FontWeight.SEMI_BOLD, 14));

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                ToggleGroup group = new ToggleGroup();
                RadioButton rbPresent = new RadioButton("Present");
                rbPresent.setToggleGroup(group);
                rbPresent.setUserData(reg.userId + ":Present");

                RadioButton rbAbsent = new RadioButton("Absent");
                rbAbsent.setToggleGroup(group);
                rbAbsent.setUserData(reg.userId + ":Absent");

                if ("Present".equalsIgnoreCase(reg.attendanceStatus)) {
                    rbPresent.setSelected(true);
                } else {
                    rbAbsent.setSelected(true);
                }

                groups.add(group);

                row.getChildren().addAll(sName, spacer, rbPresent, rbAbsent);
                rowsBox.getChildren().add(row);
            }

            Button btnSaveAtt = new Button("💾 Save Attendance");
            btnSaveAtt.setStyle("-fx-background-color: #16A34A; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6px; -fx-cursor: hand;");
            btnSaveAtt.setOnAction(e -> {
                try {
                    for (ToggleGroup g : groups) {
                        Toggle selected = g.getSelectedToggle();
                        if (selected != null) {
                            String data = (String) selected.getUserData();
                            String[] parts = data.split(":");
                            int uId = Integer.parseInt(parts[0]);
                            String attStat = parts[1];
                            eventDAO.saveAttendance(event.getEventId(), uId, attStat);
                        }
                    }
                    AlertUtils.showInfo("Success", "Attendance Saved", "Attendance recorded successfully for " + event.getTitle());
                } catch (SQLException ex) {
                    AlertUtils.showError("Database Error", "Failed to Save Attendance", ex.getMessage());
                }
            });

            container.getChildren().addAll(rowsBox, btnSaveAtt);

        } catch (SQLException e) {
            AlertUtils.showError("Error", "Failed to load attendance details", e.getMessage());
        }
    }

    // =========================================================================
    // 5. PARTICIPATION VIEW
    // =========================================================================
    private void showParticipationView() {
        mainContentArea.getChildren().clear();

        Label title = new Label("📈 Student Participation Report");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));

        Label subtitle = new Label("Overview of student engagement for your club events.");
        subtitle.setFont(Font.font("System", FontWeight.NORMAL, 14));
        subtitle.setTextFill(Color.web("#64748B"));

        VBox top = new VBox(4, title, subtitle);

        VBox contentBox = new VBox(15);
        contentBox.setPadding(new Insets(20));
        contentBox.setStyle("-fx-background-color: white; -fx-background-radius: 12px;");

        if (assignedClub == null) {
            contentBox.getChildren().add(new Label("No participation data available yet."));
        } else {
            try {
                List<StudentParticipation> list = eventDAO.getParticipationForClub(assignedClub.getClubId());

                if (list.isEmpty()) {
                    Label empty = new Label("No participation data available yet.");
                    empty.setFont(Font.font("System", FontWeight.MEDIUM, 14));
                    empty.setTextFill(Color.web("#64748B"));
                    contentBox.getChildren().add(empty);
                } else {
                    TableView<StudentParticipation> table = new TableView<>();
                    table.setPrefHeight(320);

                    TableColumn<StudentParticipation, String> nameCol = new TableColumn<>("Student Name");
                    nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name));
                    nameCol.setPrefWidth(220);

                    TableColumn<StudentParticipation, String> regCol = new TableColumn<>("Events Registered");
                    regCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().registeredCount)));
                    regCol.setPrefWidth(160);

                    TableColumn<StudentParticipation, String> attCol = new TableColumn<>("Events Attended");
                    attCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().attendedCount)));
                    attCol.setPrefWidth(160);

                    TableColumn<StudentParticipation, String> pctCol = new TableColumn<>("Attendance %");
                    pctCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.1f%%", data.getValue().percentage)));
                    pctCol.setPrefWidth(140);

                    table.getColumns().addAll(nameCol, regCol, attCol, pctCol);
                    table.setItems(FXCollections.observableArrayList(list));

                    contentBox.getChildren().add(table);
                }
            } catch (SQLException e) {
                AlertUtils.showError("Database Error", "Failed to load participation data", e.getMessage());
            }
        }

        mainContentArea.getChildren().addAll(top, contentBox);
    }

    // =========================================================================
    // CHANGE PASSWORD MODAL
    // =========================================================================
    private void openChangePasswordModal() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Change Password");

        VBox form = new VBox(14);
        form.setPadding(new Insets(24));
        form.setStyle("-fx-background-color: white;");

        PasswordField currentPwdFld = new PasswordField();
        currentPwdFld.setPromptText("Current Password");

        PasswordField newPwdFld = new PasswordField();
        newPwdFld.setPromptText("New Password");

        PasswordField confirmPwdFld = new PasswordField();
        confirmPwdFld.setPromptText("Confirm New Password");

        Button btnSave = new Button("Update Password");
        btnSave.setStyle("-fx-background-color: #6366F1; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6px; -fx-cursor: hand;");
        btnSave.setOnAction(e -> {
            String current = currentPwdFld.getText();
            String newPwd = newPwdFld.getText();
            String confirm = confirmPwdFld.getText();

            if (current.isEmpty() || newPwd.isEmpty() || confirm.isEmpty()) {
                AlertUtils.showWarning("Warning", "Empty Fields", "Please fill in all password fields.");
                return;
            }

            if (!newPwd.equals(confirm)) {
                AlertUtils.showWarning("Warning", "Password Mismatch", "New password and Confirm password do not match.");
                return;
            }

            try {
                if (!userDAO.verifyPassword(coordinatorUser.getUserId(), current)) {
                    AlertUtils.showError("Error", "Invalid Password", "Current password is incorrect.");
                    return;
                }

                userDAO.updatePassword(coordinatorUser.getUserId(), newPwd);
                AlertUtils.showInfo("Success", "Password Changed", "Your password has been changed successfully!");
                dialog.close();
            } catch (SQLException ex) {
                AlertUtils.showError("Database Error", "Failed to change password", ex.getMessage());
            }
        });

        form.getChildren().addAll(
            new Label("Current Password:"), currentPwdFld,
            new Label("New Password:"), newPwdFld,
            new Label("Confirm New Password:"), confirmPwdFld,
            btnSave
        );

        Scene scene = new Scene(form, 360, 320);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
