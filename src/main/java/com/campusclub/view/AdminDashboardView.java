package com.campusclub.view;

import com.campusclub.dao.ClubDAO;
import com.campusclub.dao.ClubDAO.ClubDetailRecord;
import com.campusclub.dao.EventDAO;
import com.campusclub.dao.EventDAO.SystemEventRecord;
import com.campusclub.dao.EventDAO.SystemParticipationRecord;
import com.campusclub.dao.UserDAO;
import com.campusclub.dao.UserDAO.StudentUserRecord;
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
 * Admin Dashboard View for system-wide campus club management, coordinator management,
 * student inspection, event oversight, participation reporting, password change, and logout.
 */
public class AdminDashboardView extends BorderPane {

    private final User adminUser;
    private final Runnable onLogout;

    private final UserDAO userDAO;
    private final ClubDAO clubDAO;
    private final EventDAO eventDAO;

    private VBox mainContentArea;

    // Sidebar navigation buttons
    private Button btnDashboard;
    private Button btnManageClubs;
    private Button btnManageCoordinators;
    private Button btnStudents;
    private Button btnEvents;
    private Button btnReports;

    public AdminDashboardView(User adminUser, Runnable onLogout) {
        this.adminUser = adminUser;
        this.onLogout = onLogout;
        this.userDAO = new UserDAO();
        this.clubDAO = new ClubDAO();
        this.eventDAO = new EventDAO();

        initializeUI();
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

        Label brandSubtitle = new Label("Admin Portal");
        brandSubtitle.setFont(Font.font("System", FontWeight.NORMAL, 12));
        brandSubtitle.setTextFill(Color.web("#94A3B8"));

        VBox brandBox = new VBox(4, brandLabel, brandSubtitle);
        brandBox.setPadding(new Insets(0, 0, 15, 5));

        // Navigation Buttons
        btnDashboard = createNavButton("📊 Dashboard");
        btnManageClubs = createNavButton("🏛 Manage Clubs");
        btnManageCoordinators = createNavButton("👔 Manage Coordinators");
        btnStudents = createNavButton("👨‍🎓 Students");
        btnEvents = createNavButton("📅 Events");
        btnReports = createNavButton("📈 Participation Reports");

        btnDashboard.setOnAction(e -> { selectNav(btnDashboard); showDashboardView(); });
        btnManageClubs.setOnAction(e -> { selectNav(btnManageClubs); showManageClubsView(); });
        btnManageCoordinators.setOnAction(e -> { selectNav(btnManageCoordinators); showManageCoordinatorsView(); });
        btnStudents.setOnAction(e -> { selectNav(btnStudents); showStudentsView(); });
        btnEvents.setOnAction(e -> { selectNav(btnEvents); showEventsView(); });
        btnReports.setOnAction(e -> { selectNav(btnReports); showReportsView(); });

        selectNav(btnDashboard);

        VBox navBox = new VBox(8, btnDashboard, btnManageClubs, btnManageCoordinators, btnStudents, btnEvents, btnReports);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Sidebar Footer - STRICT ORDER REQUIRED:
        // Admin Name -> Admin -> Change Password -> Logout
        Label nameLabel = new Label(adminUser != null ? adminUser.getName() : "Admin");
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 15));
        nameLabel.setTextFill(Color.WHITE);

        Label roleBadge = new Label("Admin");
        roleBadge.setFont(Font.font("System", FontWeight.NORMAL, 12));
        roleBadge.setTextFill(Color.web("#F43F5E"));

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
        Button[] navs = {btnDashboard, btnManageClubs, btnManageCoordinators, btnStudents, btnEvents, btnReports};
        for (Button b : navs) {
            b.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 10 14; -fx-background-radius: 8px;");
        }
        selected.setStyle("-fx-background-color: #E11D48; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10 14; -fx-background-radius: 8px;");
    }

    // =========================================================================
    // 1. MAIN DASHBOARD OVERVIEW
    // =========================================================================
    private void showDashboardView() {
        mainContentArea.getChildren().clear();

        // Welcome Header
        Label welcomeTitle = new Label("Welcome, " + adminUser.getName() + "!");
        welcomeTitle.setFont(Font.font("System", FontWeight.BOLD, 26));
        welcomeTitle.setTextFill(Color.web("#1E293B"));

        Label welcomeSub = new Label("Here's an overview of your campus club activities.");
        welcomeSub.setFont(Font.font("System", FontWeight.NORMAL, 14));
        welcomeSub.setTextFill(Color.web("#64748B"));

        VBox header = new VBox(4, welcomeTitle, welcomeSub);

        // Fetch Live System Statistic Counts from DB
        int totalStudents = 0;
        int totalClubs = 0;
        int totalEvents = 0;
        int totalAttendance = 0;

        try {
            totalStudents = userDAO.getStudentsCount();
            totalClubs = clubDAO.getClubsCount();
            totalEvents = eventDAO.getTotalEventsCount();
            totalAttendance = eventDAO.getTotalAttendanceCount();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Statistic Cards Grid
        HBox statsGrid = new HBox(16);
        statsGrid.getChildren().addAll(
            createStatCard("👨‍🎓 Total Students", String.valueOf(totalStudents), "#EEF2FF", "#4F46E5"),
            createStatCard("🏛 Total Clubs", String.valueOf(totalClubs), "#F0FDF4", "#16A34A"),
            createStatCard("📅 Total Events", String.valueOf(totalEvents), "#FEF3C7", "#D97706"),
            createStatCard("✅ Participation / Attendance", String.valueOf(totalAttendance), "#F3E8FF", "#9333EA")
        );

        // System Overview Card
        VBox overviewSection = new VBox(12);
        overviewSection.setPadding(new Insets(20));
        overviewSection.setStyle("-fx-background-color: white; -fx-background-radius: 12px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 8, 0, 0, 2);");

        Label sectionTitle = new Label("⚡ System Summary & Quick Actions");
        sectionTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        sectionTitle.setTextFill(Color.web("#1E293B"));

        Label infoLbl = new Label("As System Administrator, you have full administrative control to manage campus clubs, assign coordinators, oversee student involvement, and monitor system events.");
        infoLbl.setFont(Font.font("System", FontWeight.NORMAL, 13));
        infoLbl.setTextFill(Color.web("#475569"));
        infoLbl.setWrapText(true);

        HBox quickActions = new HBox(12);
        Button btnAddClub = new Button("➕ Add New Club");
        btnAddClub.setStyle("-fx-background-color: #4F46E5; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6px; -fx-cursor: hand;");
        btnAddClub.setOnAction(e -> { selectNav(btnManageClubs); showManageClubsView(); });

        Button btnAddCoord = new Button("➕ Add New Coordinator");
        btnAddCoord.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6px; -fx-cursor: hand;");
        btnAddCoord.setOnAction(e -> { selectNav(btnManageCoordinators); showManageCoordinatorsView(); });

        quickActions.getChildren().addAll(btnAddClub, btnAddCoord);

        overviewSection.getChildren().addAll(sectionTitle, infoLbl, quickActions);

        mainContentArea.getChildren().addAll(header, statsGrid, overviewSection);
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
        valLbl.setFont(Font.font("System", FontWeight.BOLD, 24));
        valLbl.setTextFill(Color.web(accentColor));

        card.getChildren().addAll(titleLbl, valLbl);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    // =========================================================================
    // 2. MANAGE CLUBS
    // =========================================================================
    private void showManageClubsView() {
        mainContentArea.getChildren().clear();

        Label title = new Label("🏛 Manage Campus Clubs");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));

        Label subtitle = new Label("Create, edit, or remove campus clubs and assign club coordinators.");
        subtitle.setFont(Font.font("System", FontWeight.NORMAL, 14));
        subtitle.setTextFill(Color.web("#64748B"));

        Button btnCreate = new Button("➕ Create Club");
        btnCreate.setStyle("-fx-background-color: #4F46E5; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 8px; -fx-cursor: hand;");
        btnCreate.setOnAction(e -> openClubModal(null));

        HBox topBox = new HBox(new VBox(4, title, subtitle));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topBox.getChildren().addAll(spacer, btnCreate);
        topBox.setAlignment(Pos.CENTER_LEFT);

        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: white; -fx-background-radius: 12px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 8, 0, 0, 2);");

        List<ClubDetailRecord> clubs = new ArrayList<>();
        try {
            clubs = clubDAO.getAllClubsWithDetails();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (clubs.isEmpty()) {
            Label empty = new Label("No clubs registered in the system.");
            empty.setTextFill(Color.web("#64748B"));
            empty.setPadding(new Insets(15, 0, 15, 0));
            container.getChildren().add(empty);
        } else {
            for (ClubDetailRecord club : clubs) {
                HBox card = new HBox(15);
                card.setAlignment(Pos.CENTER_LEFT);
                card.setPadding(new Insets(14, 18, 14, 18));
                card.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 8px; -fx-border-color: #E2E8F0; -fx-border-radius: 8px;");

                VBox details = new VBox(4);
                Label cTitle = new Label(club.name + " (" + club.category + ")");
                cTitle.setFont(Font.font("System", FontWeight.BOLD, 15));

                Label cCoord = new Label("👤 Coordinator: " + club.coordinatorName);
                cCoord.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
                cCoord.setTextFill(Color.web("#4F46E5"));

                Label cDesc = new Label(club.description);
                cDesc.setFont(Font.font("System", FontWeight.NORMAL, 12));
                cDesc.setTextFill(Color.web("#475569"));
                cDesc.setWrapText(true);

                details.getChildren().addAll(cTitle, cCoord, cDesc);
                HBox.setHgrow(details, Priority.ALWAYS);

                Button btnEdit = new Button("✏️ Edit");
                btnEdit.setStyle("-fx-background-color: #E0E7FF; -fx-text-fill: #4338CA; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6px;");
                btnEdit.setOnAction(e -> openClubModal(club));

                Button btnDelete = new Button("🗑 Delete");
                btnDelete.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6px;");
                btnDelete.setOnAction(e -> {
                    if (AlertUtils.showConfirmation("Confirm Delete", "Delete Club", "Are you sure you want to delete '" + club.name + "'?")) {
                        try {
                            clubDAO.deleteClub(club.clubId);
                            AlertUtils.showInfo("Success", "Club Deleted", "The club has been deleted successfully.");
                            showManageClubsView();
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

    private void openClubModal(ClubDetailRecord existing) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(existing == null ? "Create New Club" : "Edit Club");

        VBox form = new VBox(14);
        form.setPadding(new Insets(24));
        form.setStyle("-fx-background-color: white;");

        TextField nameFld = new TextField(existing != null ? existing.name : "");
        nameFld.setPromptText("e.g. Robotics Club");

        ComboBox<String> catCombo = new ComboBox<>(FXCollections.observableArrayList("Technical", "Non-Technical"));
        catCombo.setValue(existing != null ? existing.category : "Technical");

        TextArea descFld = new TextArea(existing != null ? existing.description : "");
        descFld.setPromptText("Short description of the club");
        descFld.setPrefRowCount(3);

        ComboBox<User> coordCombo = new ComboBox<>();
        try {
            List<User> coords = userDAO.getAllCoordinators();
            coordCombo.setItems(FXCollections.observableArrayList(coords));
            if (existing != null && existing.coordinatorId != null) {
                for (User u : coords) {
                    if (u.getUserId() == existing.coordinatorId) {
                        coordCombo.setValue(u);
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        coordCombo.setPromptText("Select Coordinator");

        Button btnSave = new Button(existing == null ? "Save Club" : "Update Club");
        btnSave.setStyle("-fx-background-color: #4F46E5; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6px; -fx-cursor: hand;");
        btnSave.setOnAction(e -> {
            String name = nameFld.getText().trim();
            String cat = catCombo.getValue();
            String desc = descFld.getText().trim();
            User selectedCoord = coordCombo.getValue();
            Integer coordId = selectedCoord != null ? selectedCoord.getUserId() : null;

            if (name.isEmpty() || cat == null) {
                AlertUtils.showWarning("Validation Error", "Missing Fields", "Please enter Club Name and select Category.");
                return;
            }

            try {
                if (existing == null) {
                    clubDAO.createClub(name, cat, desc, coordId);
                    AlertUtils.showInfo("Success", "Club Created", "New club created successfully!");
                } else {
                    clubDAO.updateClub(existing.clubId, name, cat, desc, coordId);
                    AlertUtils.showInfo("Success", "Club Updated", "Club updated successfully!");
                }
                dialog.close();
                showManageClubsView();
            } catch (SQLException ex) {
                AlertUtils.showError("Database Error", "Save Failed", ex.getMessage());
            }
        });

        form.getChildren().addAll(
            new Label("Club Name:"), nameFld,
            new Label("Category:"), catCombo,
            new Label("Description:"), descFld,
            new Label("Assign Coordinator:"), coordCombo,
            btnSave
        );

        Scene scene = new Scene(form, 400, 440);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // =========================================================================
    // 3. MANAGE COORDINATORS
    // =========================================================================
    private void showManageCoordinatorsView() {
        mainContentArea.getChildren().clear();

        Label title = new Label("👔 Manage Coordinators");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));

        Label subtitle = new Label("Add, edit, or manage coordinator user accounts in the system.");
        subtitle.setFont(Font.font("System", FontWeight.NORMAL, 14));
        subtitle.setTextFill(Color.web("#64748B"));

        Button btnAdd = new Button("➕ Add Coordinator");
        btnAdd.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 8px; -fx-cursor: hand;");
        btnAdd.setOnAction(e -> openCoordinatorModal(null));

        HBox topBox = new HBox(new VBox(4, title, subtitle));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topBox.getChildren().addAll(spacer, btnAdd);
        topBox.setAlignment(Pos.CENTER_LEFT);

        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: white; -fx-background-radius: 12px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 8, 0, 0, 2);");

        List<User> coords = new ArrayList<>();
        try {
            coords = userDAO.getAllCoordinators();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (coords.isEmpty()) {
            Label empty = new Label("No coordinator accounts found in the system.");
            empty.setTextFill(Color.web("#64748B"));
            empty.setPadding(new Insets(15, 0, 15, 0));
            container.getChildren().add(empty);
        } else {
            for (User coord : coords) {
                HBox card = new HBox(15);
                card.setAlignment(Pos.CENTER_LEFT);
                card.setPadding(new Insets(14, 18, 14, 18));
                card.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 8px; -fx-border-color: #E2E8F0; -fx-border-radius: 8px;");

                VBox details = new VBox(4);
                Label cName = new Label("👤 " + coord.getName() + " (ID: " + coord.getUserId() + ")");
                cName.setFont(Font.font("System", FontWeight.BOLD, 15));

                Label cEmail = new Label("✉️ Email / Roll: " + coord.getEmail());
                cEmail.setFont(Font.font("System", FontWeight.NORMAL, 13));
                cEmail.setTextFill(Color.web("#64748B"));

                details.getChildren().addAll(cName, cEmail);
                HBox.setHgrow(details, Priority.ALWAYS);

                Button btnEdit = new Button("✏️ Edit");
                btnEdit.setStyle("-fx-background-color: #E0E7FF; -fx-text-fill: #4338CA; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6px;");
                btnEdit.setOnAction(e -> openCoordinatorModal(coord));

                Button btnDelete = new Button("🗑 Remove");
                btnDelete.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6px;");
                btnDelete.setOnAction(e -> {
                    if (AlertUtils.showConfirmation("Confirm Remove", "Remove Coordinator", "Remove coordinator account for '" + coord.getName() + "'?")) {
                        try {
                            userDAO.deleteCoordinator(coord.getUserId());
                            AlertUtils.showInfo("Success", "Coordinator Removed", "Coordinator account removed.");
                            showManageCoordinatorsView();
                        } catch (SQLException ex) {
                            AlertUtils.showError("Database Error", "Remove Failed", ex.getMessage());
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

    private void openCoordinatorModal(User existing) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(existing == null ? "Add Coordinator" : "Edit Coordinator");

        VBox form = new VBox(14);
        form.setPadding(new Insets(24));
        form.setStyle("-fx-background-color: white;");

        TextField nameFld = new TextField(existing != null ? existing.getName() : "");
        nameFld.setPromptText("Coordinator Full Name");

        TextField emailFld = new TextField(existing != null ? existing.getEmail() : "");
        emailFld.setPromptText("Email or Roll Number");

        PasswordField pwdFld = new PasswordField();
        pwdFld.setPromptText("Password");

        if (existing != null) {
            pwdFld.setDisable(true);
            pwdFld.setPromptText("Password unchanged");
        }

        Button btnSave = new Button(existing == null ? "Create Coordinator" : "Update Details");
        btnSave.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6px; -fx-cursor: hand;");
        btnSave.setOnAction(e -> {
            String name = nameFld.getText().trim();
            String email = emailFld.getText().trim();
            String pwd = pwdFld.getText().trim();

            if (name.isEmpty() || email.isEmpty() || (existing == null && pwd.isEmpty())) {
                AlertUtils.showWarning("Validation Error", "Missing Fields", "Please fill in all fields.");
                return;
            }

            try {
                if (existing == null) {
                    userDAO.createCoordinator(name, email, pwd);
                    AlertUtils.showInfo("Success", "Coordinator Created", "Coordinator account created successfully!");
                } else {
                    userDAO.updateCoordinator(existing.getUserId(), name, email);
                    AlertUtils.showInfo("Success", "Coordinator Updated", "Coordinator details updated successfully!");
                }
                dialog.close();
                showManageCoordinatorsView();
            } catch (SQLException ex) {
                AlertUtils.showError("Database Error", "Save Failed", ex.getMessage());
            }
        });

        form.getChildren().addAll(
            new Label("Full Name:"), nameFld,
            new Label("Email / Roll Number:"), emailFld,
            new Label("Password:"), pwdFld,
            btnSave
        );

        Scene scene = new Scene(form, 380, 360);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // =========================================================================
    // 4. STUDENTS VIEW (PASSWORDS HIDDEN)
    // =========================================================================
    private void showStudentsView() {
        mainContentArea.getChildren().clear();

        Label title = new Label("👨‍🎓 Registered Students");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));

        Label subtitle = new Label("System-wide directory of registered student accounts.");
        subtitle.setFont(Font.font("System", FontWeight.NORMAL, 14));
        subtitle.setTextFill(Color.web("#64748B"));

        VBox top = new VBox(4, title, subtitle);

        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: white; -fx-background-radius: 12px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 8, 0, 0, 2);");

        try {
            List<StudentUserRecord> students = userDAO.getAllStudents();

            if (students.isEmpty()) {
                Label empty = new Label("No registered student accounts found.");
                empty.setTextFill(Color.web("#64748B"));
                container.getChildren().add(empty);
            } else {
                TableView<StudentUserRecord> table = new TableView<>();
                table.setPrefHeight(350);

                TableColumn<StudentUserRecord, String> idCol = new TableColumn<>("User ID");
                idCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().userId)));
                idCol.setPrefWidth(100);

                TableColumn<StudentUserRecord, String> nameCol = new TableColumn<>("Student Name");
                nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name));
                nameCol.setPrefWidth(220);

                TableColumn<StudentUserRecord, String> emailCol = new TableColumn<>("Email / Roll");
                emailCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().email));
                emailCol.setPrefWidth(260);

                TableColumn<StudentUserRecord, String> clubsCol = new TableColumn<>("Joined Clubs");
                clubsCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().joinedClubsCount)));
                clubsCol.setPrefWidth(140);

                table.getColumns().addAll(idCol, nameCol, emailCol, clubsCol);
                table.setItems(FXCollections.observableArrayList(students));

                container.getChildren().add(table);
            }
        } catch (SQLException e) {
            AlertUtils.showError("Database Error", "Failed to fetch students", e.getMessage());
        }

        mainContentArea.getChildren().addAll(top, container);
    }

    // =========================================================================
    // 5. EVENTS VIEW (SYSTEM-WIDE)
    // =========================================================================
    private void showEventsView() {
        mainContentArea.getChildren().clear();

        Label title = new Label("📅 All Campus Events");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));

        Label subtitle = new Label("System-wide overview of all events scheduled by club coordinators.");
        subtitle.setFont(Font.font("System", FontWeight.NORMAL, 14));
        subtitle.setTextFill(Color.web("#64748B"));

        VBox top = new VBox(4, title, subtitle);

        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: white; -fx-background-radius: 12px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 8, 0, 0, 2);");

        try {
            List<SystemEventRecord> events = eventDAO.getAllSystemEvents();

            if (events.isEmpty()) {
                Label empty = new Label("No events registered in the system.");
                empty.setTextFill(Color.web("#64748B"));
                container.getChildren().add(empty);
            } else {
                TableView<SystemEventRecord> table = new TableView<>();
                table.setPrefHeight(350);

                TableColumn<SystemEventRecord, String> titleCol = new TableColumn<>("Event Title");
                titleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().title));
                titleCol.setPrefWidth(200);

                TableColumn<SystemEventRecord, String> clubCol = new TableColumn<>("Organizing Club");
                clubCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().clubName));
                clubCol.setPrefWidth(160);

                TableColumn<SystemEventRecord, String> dateCol = new TableColumn<>("Date & Time");
                dateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().date + " " + data.getValue().time));
                dateCol.setPrefWidth(180);

                TableColumn<SystemEventRecord, String> venueCol = new TableColumn<>("Venue");
                venueCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().location));
                venueCol.setPrefWidth(160);

                TableColumn<SystemEventRecord, String> regCol = new TableColumn<>("Registrations");
                regCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().regCount)));
                regCol.setPrefWidth(120);

                table.getColumns().addAll(titleCol, clubCol, dateCol, venueCol, regCol);
                table.setItems(FXCollections.observableArrayList(events));

                container.getChildren().add(table);
            }
        } catch (SQLException e) {
            AlertUtils.showError("Database Error", "Failed to fetch events", e.getMessage());
        }

        mainContentArea.getChildren().addAll(top, container);
    }

    // =========================================================================
    // 6. PARTICIPATION REPORTS VIEW
    // =========================================================================
    private void showReportsView() {
        mainContentArea.getChildren().clear();

        Label title = new Label("📈 System Participation Reports");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));

        Label subtitle = new Label("System-wide analytics on student engagement across all campus clubs.");
        subtitle.setFont(Font.font("System", FontWeight.NORMAL, 14));
        subtitle.setTextFill(Color.web("#64748B"));

        VBox top = new VBox(4, title, subtitle);

        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: white; -fx-background-radius: 12px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 8, 0, 0, 2);");

        try {
            List<SystemParticipationRecord> reports = eventDAO.getSystemWideParticipation();

            if (reports.isEmpty()) {
                Label empty = new Label("No participation data available yet.");
                empty.setFont(Font.font("System", FontWeight.MEDIUM, 14));
                empty.setTextFill(Color.web("#64748B"));
                container.getChildren().add(empty);
            } else {
                TableView<SystemParticipationRecord> table = new TableView<>();
                table.setPrefHeight(350);

                TableColumn<SystemParticipationRecord, String> sNameCol = new TableColumn<>("Student Name");
                sNameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().studentName));
                sNameCol.setPrefWidth(200);

                TableColumn<SystemParticipationRecord, String> cNameCol = new TableColumn<>("Club");
                cNameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().clubName));
                cNameCol.setPrefWidth(180);

                TableColumn<SystemParticipationRecord, String> regCol = new TableColumn<>("Events Registered");
                regCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().registeredCount)));
                regCol.setPrefWidth(150);

                TableColumn<SystemParticipationRecord, String> attCol = new TableColumn<>("Events Attended");
                attCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().attendedCount)));
                attCol.setPrefWidth(150);

                TableColumn<SystemParticipationRecord, String> pctCol = new TableColumn<>("Attendance %");
                pctCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.1f%%", data.getValue().percentage)));
                pctCol.setPrefWidth(140);

                table.getColumns().addAll(sNameCol, cNameCol, regCol, attCol, pctCol);
                table.setItems(FXCollections.observableArrayList(reports));

                container.getChildren().add(table);
            }
        } catch (SQLException e) {
            AlertUtils.showError("Database Error", "Failed to fetch participation reports", e.getMessage());
        }

        mainContentArea.getChildren().addAll(top, container);
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
        btnSave.setStyle("-fx-background-color: #E11D48; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6px; -fx-cursor: hand;");
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
                if (!userDAO.verifyPassword(adminUser.getUserId(), current)) {
                    AlertUtils.showError("Error", "Invalid Password", "Current password is incorrect.");
                    return;
                }

                userDAO.updatePassword(adminUser.getUserId(), newPwd);
                AlertUtils.showInfo("Success", "Password Changed", "Your admin password has been changed successfully!");
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
