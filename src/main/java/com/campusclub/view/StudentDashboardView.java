package com.campusclub.view;

import com.campusclub.dao.ClubDAO;
import com.campusclub.dao.EventDAO;
import com.campusclub.dao.UserDAO;
import com.campusclub.model.Club;
import com.campusclub.model.Event;
import com.campusclub.model.User;
import com.campusclub.util.AlertUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

/**
 * Student Dashboard View displaying dynamic student dashboard metrics, joined clubs,
 * upcoming events with registration, sidebar navigation, change password modal, and logout.
 */
public class StudentDashboardView extends BorderPane {

    private final User studentUser;
    private final Runnable onLogout;

    private final ClubDAO clubDAO;
    private final EventDAO eventDAO;
    private final UserDAO userDAO;

    private VBox mainContentArea;
    private ScrollPane mainScrollPane;

    // Stat labels for real-time updates
    private Label myClubsValLabel;
    private Label upcomingEventsValLabel;
    private Label eventsAttendedValLabel;
    private Label participationRateValLabel;

    // Active navigation tab tracking
    private Button activeNavButton;

    public StudentDashboardView(User studentUser, Runnable onLogout) {
        this.studentUser = studentUser;
        this.onLogout = onLogout;
        this.clubDAO = new ClubDAO();
        this.eventDAO = new EventDAO();
        this.userDAO = new UserDAO();

        initializeUI();
    }

    private void initializeUI() {
        this.getStyleClass().add("dashboard-root-container");

        // 1. Left Sidebar Navigation Panel
        VBox sidebar = createSidebar();
        this.setLeft(sidebar);

        // 2. Main Scrollable Content Area
        mainContentArea = new VBox(24);
        mainContentArea.setPadding(new Insets(30, 40, 40, 40));
        mainContentArea.getStyleClass().add("hero-container");

        mainScrollPane = new ScrollPane(mainContentArea);
        mainScrollPane.setFitToWidth(true);
        mainScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        this.setCenter(mainScrollPane);

        // Load Default View: Dashboard
        loadDashboardView();
    }

    /**
     * Creates the Left Sidebar conforming to requirement order:
     * CCMS header, Navigation items, and bottom user profile with Change Password directly above Logout.
     */
    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar-pane");
        sidebar.setPrefWidth(250);
        sidebar.setPadding(new Insets(20, 16, 20, 16));

        // 1. Sidebar Brand Header
        Label brandLabel = new Label("🎓 CCMS");
        brandLabel.getStyleClass().add("sidebar-brand");
        VBox brandBox = new VBox(brandLabel);
        brandBox.getStyleClass().add("sidebar-brand-box");

        // 2. Navigation Item Buttons
        VBox navItemsBox = new VBox(6);
        navItemsBox.setPadding(new Insets(15, 0, 15, 0));

        Button dashBtn = createNavButton("📊 Dashboard", () -> loadDashboardView());

        // Clubs Navigation Item without triangle symbol
        Button clubsBtn = createNavButton("🎭 Clubs", () -> loadClubsFiltered("Technical", null));

        VBox clubsSubmenuBox = new VBox(4);
        clubsSubmenuBox.setPadding(new Insets(4, 0, 4, 10));
        clubsSubmenuBox.setVisible(false);
        clubsSubmenuBox.setManaged(false);

        Button techSubBtn = createSubNavButton("💻 Technical", () -> loadClubsFiltered("Technical", null));
        Button nonTechSubBtn = createSubNavButton("🎨 Non-Technical", () -> loadClubsFiltered("Non-Technical", null));

        clubsSubmenuBox.getChildren().addAll(techSubBtn, nonTechSubBtn);

        VBox clubsNavGroup = new VBox(0, clubsBtn, clubsSubmenuBox);

        // Hover Action: Hovering on Clubs reveals Technical and Non-Technical instantly without clicking
        clubsNavGroup.setOnMouseEntered(e -> {
            clubsSubmenuBox.setVisible(true);
            clubsSubmenuBox.setManaged(true);
        });

        clubsNavGroup.setOnMouseExited(e -> {
            clubsSubmenuBox.setVisible(false);
            clubsSubmenuBox.setManaged(false);
        });

        Button eventsBtn = createNavButton("📅 Events", () -> loadEventsView());
        Button participationBtn = createNavButton("🏆 My Participation", () -> loadParticipationView());
        Button chatbotBtn = createNavButton("🤖 Chatbot", () -> loadChatbotView());

        navItemsBox.getChildren().addAll(dashBtn, clubsNavGroup, eventsBtn, participationBtn, chatbotBtn);

        // Default active tab
        setActiveNav(dashBtn);

        // Spacer to push bottom profile card down
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // 3. Bottom User Profile Box
        VBox bottomBox = new VBox(10);
        bottomBox.getStyleClass().add("sidebar-user-box");
        bottomBox.setAlignment(Pos.CENTER_LEFT);

        String studentName = (studentUser != null && studentUser.getName() != null) ? studentUser.getName() : "Student";
        Label nameLabel = new Label(studentName);
        nameLabel.getStyleClass().add("sidebar-user-name");

        Label roleLabel = new Label("Student");
        roleLabel.getStyleClass().add("sidebar-user-role");

        VBox userDetailsBox = new VBox(2, nameLabel, roleLabel);

        Button changePasswordBtn = new Button("🔑 Change Password");
        changePasswordBtn.getStyleClass().add("sidebar-action-btn");
        changePasswordBtn.setOnAction(e -> openChangePasswordDialog());

        Button logoutBtn = new Button("🚪 Logout");
        logoutBtn.getStyleClass().add("sidebar-logout-btn");
        logoutBtn.setOnAction(e -> handleLogoutAction());

        bottomBox.getChildren().addAll(
            userDetailsBox,
            changePasswordBtn,
            logoutBtn
        );

        sidebar.getChildren().addAll(brandBox, navItemsBox, spacer, bottomBox);
        return sidebar;
    }

    private Button createNavButton(String title, Runnable action) {
        Button btn = new Button(title);
        btn.getStyleClass().add("sidebar-nav-item");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> {
            setActiveNav(btn);
            if (action != null) action.run();
        });
        return btn;
    }

    private void setActiveNav(Button btn) {
        if (activeNavButton != null) {
            activeNavButton.getStyleClass().remove("sidebar-nav-item-active");
            activeNavButton.getStyleClass().remove("sidebar-subnav-item-active");
        }
        activeNavButton = btn;
        if (!activeNavButton.getStyleClass().contains("sidebar-nav-item-active")) {
            activeNavButton.getStyleClass().add("sidebar-nav-item-active");
        }
    }

    private Button createSubNavButton(String title, Runnable action) {
        Button btn = new Button(title);
        btn.getStyleClass().add("sidebar-subnav-item");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> {
            setActiveSubNav(btn);
            if (action != null) action.run();
        });
        return btn;
    }

    private void setActiveSubNav(Button btn) {
        if (activeNavButton != null) {
            activeNavButton.getStyleClass().remove("sidebar-nav-item-active");
            activeNavButton.getStyleClass().remove("sidebar-subnav-item-active");
        }
        activeNavButton = btn;
        if (!activeNavButton.getStyleClass().contains("sidebar-subnav-item-active")) {
            activeNavButton.getStyleClass().add("sidebar-subnav-item-active");
        }
    }

    /**
     * Creates a dark flyout ContextMenu for "Clubs" with 2 categories (Technical, Non-Technical)
     * and nested flyout club sub-items.
     */
    private ContextMenu createClubsFlyoutMenu(Button clubsBtn) {
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getStyleClass().add("context-menu");

        // Level 1 Category: Technical
        Menu techMenu = new Menu("💻 Technical");
        techMenu.getStyleClass().add("menu");

        // Level 2 Sub-items: Technical Clubs
        MenuItem scrsItem = new MenuItem("⚙️ SCRS");
        scrsItem.setOnAction(e -> {
            setActiveNav(clubsBtn);
            loadClubsFiltered("Technical", "SCRS");
        });

        MenuItem smcItem = new MenuItem("🤖 IEEE SMC");
        smcItem.setOnAction(e -> {
            setActiveNav(clubsBtn);
            loadClubsFiltered("Technical", "IEEE SMC");
        });

        techMenu.getItems().addAll(scrsItem, smcItem);

        // Level 1 Category: Non-Technical
        Menu nonTechMenu = new Menu("🎨 Non-Technical");
        nonTechMenu.getStyleClass().add("menu");

        // Level 2 Sub-items: Non-Technical Clubs
        MenuItem vishakaItem = new MenuItem("🌸 Vishaka");
        vishakaItem.setOnAction(e -> {
            setActiveNav(clubsBtn);
            loadClubsFiltered("Non-Technical", "Vishaka");
        });

        nonTechMenu.getItems().addAll(vishakaItem);

        contextMenu.getItems().addAll(techMenu, nonTechMenu);
        return contextMenu;
    }

    // =========================================================================
    // MAIN DASHBOARD VIEW
    // =========================================================================

    private void loadDashboardView() {
        mainContentArea.getChildren().clear();

        // 1. Welcome Header
        String name = (studentUser != null && studentUser.getName() != null) ? studentUser.getName() : "Student";
        Label welcomeTitle = new Label("Welcome, " + name + "!");
        welcomeTitle.getStyleClass().add("login-heading");

        Label welcomeSubtitle = new Label("Here's what's happening in your campus clubs.");
        welcomeSubtitle.getStyleClass().add("login-subheading");

        VBox welcomeHeader = new VBox(4, welcomeTitle, welcomeSubtitle);

        // 2. Four Statistic Cards
        HBox statCardsRow = createStatCardsRow();

        // 3. My Clubs Section
        VBox myClubsSection = createMyClubsSection();

        // 4. Upcoming Events Section
        VBox upcomingEventsSection = createUpcomingEventsSection();

        mainContentArea.getChildren().addAll(
            welcomeHeader,
            statCardsRow,
            myClubsSection,
            upcomingEventsSection
        );
    }

    /**
     * Creates four statistic cards: My Clubs, Upcoming Events, Events Attended, Participation Rate.
     */
    private HBox createStatCardsRow() {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER);

        int userId = studentUser != null ? studentUser.getUserId() : 0;

        int myClubsCount = 0;
        int upcomingEventsCount = 0;
        int eventsAttendedCount = 0;
        double participationRate = 0.0;

        try {
            myClubsCount = clubDAO.getJoinedClubsCount(userId);
            upcomingEventsCount = eventDAO.getUpcomingEventsCount();
            eventsAttendedCount = eventDAO.getEventsAttendedCount(userId);
            participationRate = eventDAO.getParticipationRate(userId);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        myClubsValLabel = new Label(String.valueOf(myClubsCount));
        myClubsValLabel.getStyleClass().add("stat-card-value");
        VBox card1 = createStatCard("1. My Clubs", myClubsValLabel, "Clubs Joined", "🎭");

        upcomingEventsValLabel = new Label(String.valueOf(upcomingEventsCount));
        upcomingEventsValLabel.getStyleClass().add("stat-card-value");
        VBox card2 = createStatCard("2. Upcoming Events", upcomingEventsValLabel, "Active Events", "📅");

        eventsAttendedValLabel = new Label(String.valueOf(eventsAttendedCount));
        eventsAttendedValLabel.getStyleClass().add("stat-card-value");
        VBox card3 = createStatCard("3. Events Attended", eventsAttendedValLabel, "Attended", "🏆");

        participationRateValLabel = new Label(String.format("%.0f%%", participationRate));
        participationRateValLabel.getStyleClass().add("stat-card-value");
        VBox card4 = createStatCard("4. Participation Rate", participationRateValLabel, "Involvement", "📊");

        HBox.setHgrow(card1, Priority.ALWAYS);
        HBox.setHgrow(card2, Priority.ALWAYS);
        HBox.setHgrow(card3, Priority.ALWAYS);
        HBox.setHgrow(card4, Priority.ALWAYS);

        row.getChildren().addAll(card1, card2, card3, card4);
        return row;
    }

    private VBox createStatCard(String title, Label valLabel, String subLabel, String icon) {
        VBox card = new VBox(8);
        card.getStyleClass().add("stat-card");

        Label iconTitle = new Label(icon + "  " + title);
        iconTitle.getStyleClass().add("stat-card-title");

        Label sub = new Label(subLabel);
        sub.getStyleClass().add("stat-card-subtitle");

        card.getChildren().addAll(iconTitle, valLabel, sub);
        return card;
    }

    /**
     * Refreshes the 4 statistic card values from MySQL database.
     */
    private void refreshStatCards() {
        if (studentUser == null) return;
        int userId = studentUser.getUserId();
        try {
            int myClubsCount = clubDAO.getJoinedClubsCount(userId);
            int upcomingEventsCount = eventDAO.getUpcomingEventsCount();
            int eventsAttendedCount = eventDAO.getEventsAttendedCount(userId);
            double participationRate = eventDAO.getParticipationRate(userId);

            if (myClubsValLabel != null) myClubsValLabel.setText(String.valueOf(myClubsCount));
            if (upcomingEventsValLabel != null) upcomingEventsValLabel.setText(String.valueOf(upcomingEventsCount));
            if (eventsAttendedValLabel != null) eventsAttendedValLabel.setText(String.valueOf(eventsAttendedCount));
            if (participationRateValLabel != null) participationRateValLabel.setText(String.format("%.0f%%", participationRate));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates "My Clubs" section displaying clubs joined by the logged-in student.
     */
    private VBox createMyClubsSection() {
        VBox section = new VBox(16);
        section.getStyleClass().add("dashboard-section-card");

        Label titleLabel = new Label("My Clubs");
        titleLabel.getStyleClass().add("dashboard-section-title");

        VBox clubsListContainer = new VBox(10);
        int userId = studentUser != null ? studentUser.getUserId() : 0;

        try {
            List<Club> joinedClubs = clubDAO.getClubsForUser(userId);
            if (joinedClubs.isEmpty()) {
                Label emptyLabel = new Label("No clubs joined yet");
                emptyLabel.getStyleClass().add("card-desc");
                clubsListContainer.getChildren().add(emptyLabel);
            } else {
                for (Club club : joinedClubs) {
                    HBox clubRow = createClubRow(club);
                    clubsListContainer.getChildren().add(clubRow);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Label errLabel = new Label("Could not load clubs: " + e.getMessage());
            errLabel.getStyleClass().add("card-desc");
            clubsListContainer.getChildren().add(errLabel);
        }

        section.getChildren().addAll(titleLabel, clubsListContainer);
        return section;
    }

    private HBox createClubRow(Club club) {
        HBox row = new HBox();
        row.getStyleClass().add("club-item-card");
        row.setAlignment(Pos.CENTER_LEFT);

        String icon = club.getIcon() != null ? club.getIcon() : "🎭";
        Label nameLbl = new Label(icon + "  " + club.getName());
        nameLbl.getStyleClass().add("card-title");

        Label catLbl = new Label(" • " + club.getCategory());
        catLbl.getStyleClass().add("card-desc");

        HBox leftBox = new HBox(6, nameLbl, catLbl);
        leftBox.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusBadge = new Label("✓ " + (club.getMembershipStatus() != null ? club.getMembershipStatus() : "Active Member"));
        statusBadge.getStyleClass().add("status-badge-active");

        row.getChildren().addAll(leftBox, spacer, statusBadge);
        return row;
    }

    /**
     * Creates "Upcoming Events" section displaying event name, date, time, and Register/Registered button.
     */
    private VBox createUpcomingEventsSection() {
        VBox section = new VBox(16);
        section.getStyleClass().add("dashboard-section-card");

        Label titleLabel = new Label("Upcoming Events");
        titleLabel.getStyleClass().add("dashboard-section-title");

        VBox eventsListContainer = new VBox(12);
        int userId = studentUser != null ? studentUser.getUserId() : 0;

        try {
            List<Event> upcomingEvents = eventDAO.getUpcomingEventsForUser(userId);
            if (upcomingEvents.isEmpty()) {
                Label emptyLabel = new Label("No upcoming events");
                emptyLabel.getStyleClass().add("card-desc");
                eventsListContainer.getChildren().add(emptyLabel);
            } else {
                for (Event event : upcomingEvents) {
                    HBox eventRow = createEventRow(event);
                    eventsListContainer.getChildren().add(eventRow);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Label errLabel = new Label("Could not load upcoming events: " + e.getMessage());
            errLabel.getStyleClass().add("card-desc");
            eventsListContainer.getChildren().add(errLabel);
        }

        section.getChildren().addAll(titleLabel, eventsListContainer);
        return section;
    }

    private HBox createEventRow(Event event) {
        HBox row = new HBox();
        row.getStyleClass().add("event-item-card");
        row.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label(event.getTitle());
        titleLbl.getStyleClass().add("card-title");

        Label metaLbl = new Label("📅 " + event.getEventDate() + "  |  ⏰ " + event.getEventTime() + "  |  📍 " + event.getLocation());
        metaLbl.getStyleClass().add("card-desc");

        VBox infoBox = new VBox(4, titleLbl, metaLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Node actionNode;
        if (event.isRegistered()) {
            Label regBadge = new Label("✓ Registered");
            regBadge.getStyleClass().add("registered-badge");
            actionNode = regBadge;
        } else {
            Button regBtn = new Button("Register");
            regBtn.getStyleClass().add("register-btn");
            regBtn.setOnAction(e -> handleEventRegistration(event, row));
            actionNode = regBtn;
        }

        row.getChildren().addAll(infoBox, spacer, actionNode);
        return row;
    }

    /**
     * Handles student event registration and updates UI in real-time.
     */
    private void handleEventRegistration(Event event, HBox eventRow) {
        if (studentUser == null) return;
        int userId = studentUser.getUserId();

        try {
            boolean success = eventDAO.registerForEvent(userId, event.getEventId());
            if (success) {
                event.setRegistered(true);
                AlertUtils.showInfo(
                    "Registration Successful",
                    "Event Registration Confirmed",
                    "You have successfully registered for '" + event.getTitle() + "'!"
                );

                // Update event row button to Registered badge
                int lastIdx = eventRow.getChildren().size() - 1;
                Label regBadge = new Label("✓ Registered");
                regBadge.getStyleClass().add("registered-badge");
                eventRow.getChildren().set(lastIdx, regBadge);

                // Refresh stat cards
                refreshStatCards();
            } else {
                AlertUtils.showWarning(
                    "Already Registered",
                    "Duplicate Registration",
                    "You are already registered for this event."
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtils.showError(
                "Database Error",
                "Registration Failed",
                "Error registering for event: " + e.getMessage()
            );
        }
    }

    // =========================================================================
    // CHANGE PASSWORD MODAL DIALOG
    // =========================================================================

    /**
     * Opens the Change Password Modal containing:
     * Current Password, New Password, Confirm New Password, Show/Hide password toggle, Change Password button, Cancel button.
     */
    private void openChangePasswordDialog() {
        if (studentUser == null) return;

        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Change Password - Campus Club Management System");
        dialogStage.setResizable(false);

        VBox root = new VBox(18);
        root.setPadding(new Insets(30, 35, 30, 35));
        root.setStyle("-fx-background-color: #FFFFFF; -fx-font-family: 'Segoe UI', sans-serif;");

        Label headerTitle = new Label("Change Password");
        headerTitle.getStyleClass().add("login-heading");

        Label headerSub = new Label("Update your account security credentials");
        headerSub.getStyleClass().add("login-subheading");

        VBox headerBox = new VBox(4, headerTitle, headerSub);

        // 1. Current Password
        Label currentLbl = new Label("Current Password");
        currentLbl.getStyleClass().add("form-label");

        PasswordField currentPwdField = new PasswordField();
        currentPwdField.setPromptText("Enter current password");
        currentPwdField.getStyleClass().add("form-input");

        TextField currentTxtField = new TextField();
        currentTxtField.setPromptText("Enter current password");
        currentTxtField.getStyleClass().add("form-input");
        currentTxtField.setVisible(false);
        currentTxtField.setManaged(false);

        bindFields(currentPwdField, currentTxtField);

        // 2. New Password
        Label newLbl = new Label("New Password");
        newLbl.getStyleClass().add("form-label");

        PasswordField newPwdField = new PasswordField();
        newPwdField.setPromptText("Enter new password");
        newPwdField.getStyleClass().add("form-input");

        TextField newTxtField = new TextField();
        newTxtField.setPromptText("Enter new password");
        newTxtField.getStyleClass().add("form-input");
        newTxtField.setVisible(false);
        newTxtField.setManaged(false);

        bindFields(newPwdField, newTxtField);

        // 3. Confirm New Password
        Label confirmLbl = new Label("Confirm New Password");
        confirmLbl.getStyleClass().add("form-label");

        PasswordField confirmPwdField = new PasswordField();
        confirmPwdField.setPromptText("Confirm new password");
        confirmPwdField.getStyleClass().add("form-input");

        TextField confirmTxtField = new TextField();
        confirmTxtField.setPromptText("Confirm new password");
        confirmTxtField.getStyleClass().add("form-input");
        confirmTxtField.setVisible(false);
        confirmTxtField.setManaged(false);

        bindFields(confirmPwdField, confirmTxtField);

        // Show / Hide Password Checkbox
        CheckBox showPwdCheckBox = new CheckBox("Show Passwords");
        showPwdCheckBox.setStyle("-fx-text-fill: #667085; -fx-font-size: 12px;");
        showPwdCheckBox.setOnAction(e -> {
            boolean visible = showPwdCheckBox.isSelected();

            toggleFieldVisibility(currentPwdField, currentTxtField, visible);
            toggleFieldVisibility(newPwdField, newTxtField, visible);
            toggleFieldVisibility(confirmPwdField, confirmTxtField, visible);
        });

        // Action Buttons
        Button changeBtn = new Button("Change Password");
        changeBtn.getStyleClass().add("login-submit-button");
        changeBtn.setMaxWidth(Double.MAX_VALUE);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("back-button");
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setOnAction(e -> dialogStage.close());

        changeBtn.setOnAction(e -> {
            String currentPwd = showPwdCheckBox.isSelected() ? currentTxtField.getText() : currentPwdField.getText();
            String newPwd = showPwdCheckBox.isSelected() ? newTxtField.getText() : newPwdField.getText();
            String confirmPwd = showPwdCheckBox.isSelected() ? confirmTxtField.getText() : confirmPwdField.getText();

            // Validation 1: Check empty fields
            if (currentPwd == null || currentPwd.trim().isEmpty() ||
                newPwd == null || newPwd.trim().isEmpty() ||
                confirmPwd == null || confirmPwd.trim().isEmpty()) {
                AlertUtils.showWarning(
                    "Validation Error",
                    "Empty Fields",
                    "Please fill in all password fields."
                );
                return;
            }

            // Validation 2: New password and confirm must match
            if (!newPwd.equals(confirmPwd)) {
                AlertUtils.showWarning(
                    "Validation Error",
                    "Password Mismatch",
                    "New Password and Confirm New Password do not match."
                );
                return;
            }

            // Validation 3: Verify current password against MySQL DB
            try {
                boolean isCurrentValid = userDAO.verifyPassword(studentUser.getUserId(), currentPwd.trim());
                if (!isCurrentValid) {
                    AlertUtils.showError(
                        "Validation Error",
                        "Incorrect Current Password",
                        "The current password you entered is incorrect."
                    );
                    return;
                }

                // Execute Update in MySQL users table for currently logged-in student ONLY
                boolean updated = userDAO.updatePassword(studentUser.getUserId(), newPwd.trim());
                if (updated) {
                    studentUser.setPassword(newPwd.trim()); // Update memory object

                    AlertUtils.showInfo(
                        "Success",
                        "Password Changed",
                        "Your password has been successfully updated!"
                    );

                    // Clear fields and close dialog
                    currentPwdField.clear();
                    currentTxtField.clear();
                    newPwdField.clear();
                    newTxtField.clear();
                    confirmPwdField.clear();
                    confirmTxtField.clear();

                    dialogStage.close();
                } else {
                    AlertUtils.showError(
                        "Update Failed",
                        "Database Error",
                        "Failed to update password in database."
                    );
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
                AlertUtils.showError(
                    "Database Error",
                    "Error Updating Password",
                    "Error: " + ex.getMessage()
                );
            }
        });

        HBox btnBox = new HBox(12, cancelBtn, changeBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        VBox formBox = new VBox(12,
            new VBox(4, currentLbl, currentPwdField, currentTxtField),
            new VBox(4, newLbl, newPwdField, newTxtField),
            new VBox(4, confirmLbl, confirmPwdField, confirmTxtField),
            showPwdCheckBox
        );

        root.getChildren().addAll(headerBox, formBox, btnBox);

        Scene dialogScene = new Scene(root, 420, 520);
        if (getScene() != null && !getScene().getStylesheets().isEmpty()) {
            dialogScene.getStylesheets().addAll(getScene().getStylesheets());
        }

        dialogStage.setScene(dialogScene);
        dialogStage.showAndWait();
    }

    private void bindFields(PasswordField pwd, TextField txt) {
        pwd.textProperty().addListener((obs, oldV, newV) -> {
            if (!txt.getText().equals(newV)) txt.setText(newV);
        });
        txt.textProperty().addListener((obs, oldV, newV) -> {
            if (!pwd.getText().equals(newV)) pwd.setText(newV);
        });
    }

    private void toggleFieldVisibility(PasswordField pwd, TextField txt, boolean visible) {
        if (visible) {
            pwd.setVisible(false);
            pwd.setManaged(false);
            txt.setVisible(true);
            txt.setManaged(true);
        } else {
            txt.setVisible(false);
            txt.setManaged(false);
            pwd.setVisible(true);
            pwd.setManaged(true);
        }
    }

    private void handleLogoutAction() {
        if (onLogout != null) {
            onLogout.run();
        }
    }

    // =========================================================================
    // AUXILIARY SIDEBAR VIEWS (Clubs, Events, Participation, Chatbot)
    // =========================================================================

    private void loadClubsView() {
        loadClubsFiltered("Technical", null);
    }

    private void loadClubsFiltered(String categoryFilter, String clubNameFilter) {
        mainContentArea.getChildren().clear();

        String activeCat = categoryFilter != null ? categoryFilter : "Technical";
        String titleText = activeCat + " Clubs";
        String subText = "Explore active campus " + activeCat.toLowerCase() + " clubs and organizations";

        if (clubNameFilter != null) {
            titleText = "Club: " + clubNameFilter;
            subText = "Explore details and membership status for " + clubNameFilter;
        }

        Label title = new Label(titleText);
        title.getStyleClass().add("login-heading");
        Label sub = new Label(subText);
        sub.getStyleClass().add("login-subheading");

        VBox header = new VBox(4, title, sub);

        // Top Control Bar: Search Input + Filter Button ONLY (Pills removed per specification)
        HBox searchFilterBar = new HBox(12);
        searchFilterBar.setAlignment(Pos.CENTER_LEFT);

        TextField searchInput = new TextField();
        searchInput.setPromptText("Search clubs...");
        searchInput.getStyleClass().add("form-input");
        HBox.setHgrow(searchInput, Priority.ALWAYS);

        Button filterBtn = new Button("Filter ⚡");
        filterBtn.getStyleClass().add("filter-btn-outline");

        searchFilterBar.getChildren().addAll(searchInput, filterBtn);

        // Responsive Cards Grid Container (3 compact cards per row)
        FlowPane cardsGrid = new FlowPane();
        cardsGrid.setHgap(20);
        cardsGrid.setVgap(20);

        int userId = studentUser != null ? studentUser.getUserId() : 0;

        Runnable populateCards = () -> {
            cardsGrid.getChildren().clear();
            String query = searchInput.getText() != null ? searchInput.getText().toLowerCase().trim() : "";

            try {
                List<Club> allClubs;
                try {
                    allClubs = clubDAO.getAllClubs();
                } catch (Exception ex) {
                    allClubs = new java.util.ArrayList<>();
                }

                List<Club> joinedClubs;
                try {
                    joinedClubs = clubDAO.getClubsForUser(userId);
                } catch (Exception ex) {
                    joinedClubs = new java.util.ArrayList<>();
                }

                List<Club> displayClubs = new java.util.ArrayList<>();

                // SCRS (exactly 1 instance)
                Club scrsClub = allClubs.stream()
                    .filter(c -> c.getName().equalsIgnoreCase("SCRS") || c.getName().toLowerCase().contains("scrs"))
                    .findFirst()
                    .orElse(new Club(1, "SCRS", "Technical",
                        "Encouraging undergraduate and postgraduate students to explore advanced computing domains and publish scientific papers.",
                        "⚙️", "Not Joined"));
                scrsClub.setName("SCRS");
                scrsClub.setCategory("Technical");
                scrsClub.setDescription("Encouraging undergraduate and postgraduate students to explore advanced computing domains and publish scientific papers.");
                displayClubs.add(scrsClub);

                // IEEE SMC (exactly 1 instance)
                Club smcClub = allClubs.stream()
                    .filter(c -> c.getName().equalsIgnoreCase("IEEE SMC") || c.getName().toLowerCase().contains("smc"))
                    .findFirst()
                    .orElse(new Club(2, "IEEE SMC", "Technical",
                        "Brainstorming and building real-world application prototypes during collaborative campus marathons.",
                        "🤖", "Not Joined"));
                smcClub.setName("IEEE SMC");
                smcClub.setCategory("Technical");
                smcClub.setDescription("Brainstorming and building real-world application prototypes during collaborative campus marathons.");
                displayClubs.add(smcClub);

                // Vishaka (exactly 1 instance)
                Club vishakaClub = allClubs.stream()
                    .filter(c -> c.getName().equalsIgnoreCase("Vishaka") || c.getName().toLowerCase().contains("vishaka"))
                    .findFirst()
                    .orElse(new Club(4, "Vishaka", "Non-Technical",
                        "The club acts as a creative hub dedicated to blending cultural tradition with innovative entertainment, fostering artistic expression among university students.",
                        "🌸", "Not Joined"));
                vishakaClub.setName("Vishaka");
                vishakaClub.setCategory("Non-Technical");
                vishakaClub.setDescription("The club acts as a creative hub dedicated to blending cultural tradition with innovative entertainment, fostering artistic expression among university students.");
                displayClubs.add(vishakaClub);

                for (Club club : displayClubs) {
                    boolean isVishaka = club.getName().equalsIgnoreCase("Vishaka");
                    boolean isSCRS = club.getName().equalsIgnoreCase("SCRS");
                    boolean isSMC = club.getName().equalsIgnoreCase("IEEE SMC");

                    boolean isNonTechCategory = categoryFilter != null && 
                        ("Non-Technical".equalsIgnoreCase(categoryFilter) || "Non Technical".equalsIgnoreCase(categoryFilter));
                    boolean isTechCategory = categoryFilter == null || "Technical".equalsIgnoreCase(categoryFilter);

                    if (isNonTechCategory) {
                        // Non-Technical view MUST display ONLY Vishaka card
                        if (!isVishaka) {
                            continue;
                        }
                    } else if (isTechCategory) {
                        // Technical view MUST display EXACTLY TWO cards: SCRS and IEEE SMC
                        if (!isSCRS && !isSMC) {
                            continue;
                        }
                    } else if (categoryFilter != null && !club.getCategory().equalsIgnoreCase(categoryFilter)) {
                        continue;
                    }

                    if (clubNameFilter != null && !club.getName().toLowerCase().contains(clubNameFilter.toLowerCase())) {
                        continue;
                    }
                    if (!query.isEmpty()) {
                        boolean nameMatch = club.getName().toLowerCase().contains(query);
                        boolean descMatch = club.getDescription() != null && club.getDescription().toLowerCase().contains(query);
                        boolean catMatch = club.getCategory() != null && club.getCategory().toLowerCase().contains(query);
                        if (!nameMatch && !descMatch && !catMatch) {
                            continue;
                        }
                    }

                    boolean isJoined = joinedClubs.stream().anyMatch(c -> c.getClubId() == club.getClubId());
                    VBox clubCard = createCompactClubCard(club, isJoined, userId, categoryFilter, clubNameFilter);
                    cardsGrid.getChildren().add(clubCard);
                }

                if (cardsGrid.getChildren().isEmpty()) {
                    VBox emptyCard = new VBox(10);
                    emptyCard.getStyleClass().add("dashboard-section-card");
                    Label emptyLbl = new Label("No clubs matching your search criteria found.");
                    emptyLbl.getStyleClass().add("card-desc");
                    emptyCard.getChildren().add(emptyLbl);
                    cardsGrid.getChildren().add(emptyCard);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        // Real-time live search filter listener
        searchInput.textProperty().addListener((obs, oldVal, newVal) -> populateCards.run());

        // Initial Populate
        populateCards.run();

        mainContentArea.getChildren().addAll(header, searchFilterBar, cardsGrid);
    }

    private VBox createCompactClubCard(Club club, boolean isJoined, int userId, String currentCatFilter, String currentNameFilter) {
        boolean isVishaka = club.getName().equalsIgnoreCase("Vishaka");
        boolean isSCRS = club.getName().equalsIgnoreCase("SCRS");
        boolean isSMC = club.getName().equalsIgnoreCase("IEEE SMC") || club.getName().contains("SMC");

        VBox card = new VBox(12);
        card.getStyleClass().add("compact-club-card");
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefWidth(290);
        card.setMaxWidth(320);
        card.setPadding(new Insets(24, 20, 24, 20));

        // Header Avatar Icon Box
        Node iconNode = null;
        String logoPath = null;
        String fallbackEmoji = "🎭";

        if (isVishaka) {
            logoPath = "/assets/images/vishaka_logo.png";
            fallbackEmoji = "🌸";
        } else if (isSCRS) {
            logoPath = "/assets/images/scrs_logo.png";
            fallbackEmoji = "⚙️";
        } else if (isSMC) {
            logoPath = "/assets/images/ieee_smc_logo.png";
            fallbackEmoji = "🤖";
        }

        if (logoPath != null) {
            try {
                InputStream stream = getClass().getResourceAsStream(logoPath);
                if (stream != null) {
                    Image img = new Image(stream);
                    ImageView imgView = new ImageView(img);
                    imgView.setFitWidth(65);
                    imgView.setFitHeight(65);
                    imgView.setPreserveRatio(true);
                    imgView.setSmooth(true);
                    iconNode = imgView;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        if (iconNode == null) {
            String icon = club.getIcon() != null ? club.getIcon() : fallbackEmoji;
            Label iconLbl = new Label(icon);
            iconLbl.setStyle("-fx-font-size: 32px;");
            iconNode = iconLbl;
        }

        VBox avatarBox = new VBox(iconNode);
        avatarBox.getStyleClass().add("club-avatar-box");
        avatarBox.setAlignment(Pos.CENTER);

        // Clean Title & Subtitle Mapping
        String displayTitle;
        String displaySubtitle;

        if (isSCRS) {
            displayTitle = "SCRS";
            displaySubtitle = "Soft Computing Research Society";
        } else if (isSMC) {
            displayTitle = "IEEE SMC";
            displaySubtitle = "Systems, Man, and Cybernetics";
        } else if (isVishaka) {
            displayTitle = "Vishaka";
            displaySubtitle = "Creative Hub & Performing Arts";
        } else {
            displayTitle = club.getName();
            displaySubtitle = club.getCategory() + " Club";
        }

        Label titleLbl = new Label(displayTitle);
        titleLbl.getStyleClass().add("compact-card-title");

        // Category Badge
        boolean isTech = "Technical".equalsIgnoreCase(club.getCategory());
        Label catBadge = new Label(club.getCategory());
        catBadge.getStyleClass().add(isTech ? "club-badge-tech" : "club-badge-nontech");

        Label subtitleLbl = new Label(displaySubtitle);
        subtitleLbl.getStyleClass().add("compact-card-subtitle");

        // Member Count dynamically retrieved from MySQL DB
        int memberCount = 0;
        try {
            memberCount = clubDAO.getClubMemberCount(club.getClubId());
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        Label membersLbl = new Label(memberCount + (memberCount == 1 ? " member" : " members"));
        membersLbl.getStyleClass().add("compact-card-members");

        VBox textStack = new VBox(4, titleLbl, catBadge, subtitleLbl, membersLbl);
        textStack.setAlignment(Pos.CENTER);

        // Action Buttons Side-by-Side HBox with spacing="10" and alignment="CENTER"
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button viewDetailsBtn = new Button("View Details");
        viewDetailsBtn.getStyleClass().add("view-details-btn");

        if (isSCRS) {
            viewDetailsBtn.setOnAction(e -> handleViewSCRSDetails(club, isJoined, userId));
        } else if (isSMC) {
            viewDetailsBtn.setOnAction(e -> handleViewSMCDetails(club, isJoined, userId));
        } else {
            viewDetailsBtn.setOnAction(e -> openClubDetailsModal(club, isJoined, userId));
        }

        Button joinBtn;
        if (isJoined) {
            joinBtn = new Button("✓ Joined");
            joinBtn.getStyleClass().add("registered-badge");
        } else {
            joinBtn = new Button("Join");
            joinBtn.getStyleClass().add("join-pill-btn");
            joinBtn.setOnAction(e -> {
                try {
                    boolean success = clubDAO.joinClub(userId, club.getClubId());
                    if (success) {
                        AlertUtils.showInfo("Joined Club", "Success", "You have successfully joined " + displayTitle + "!");
                        loadClubsFiltered(currentCatFilter, currentNameFilter);
                        refreshStatCards();
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            });
        }

        buttonBox.getChildren().addAll(viewDetailsBtn, joinBtn);

        card.getChildren().addAll(avatarBox, textStack, buttonBox);

        return card;
    }

    private void handleViewSCRSDetails(Club club, boolean isJoined, int userId) {
        openClubDetailsModal(club, isJoined, userId);
    }

    private void handleViewSMCDetails(Club club, boolean isJoined, int userId) {
        openClubDetailsModal(club, isJoined, userId);
    }

    private void handleJoinVishaka(Club club, int userId, String currentCatFilter, String currentNameFilter) {
        try {
            boolean success = clubDAO.joinClub(userId, club.getClubId());
            if (success) {
                AlertUtils.showInfo("Joined Club", "Success", "You have successfully joined Vishaka!");
                loadClubsFiltered(currentCatFilter, currentNameFilter);
                refreshStatCards();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            AlertUtils.showError("Database Error", "Join Failed", "Could not join Vishaka: " + ex.getMessage());
        }
    }

    /**
     * Opens rich modal dialog with full club details.
     */
    private void openClubDetailsModal(Club club, boolean isJoined, int userId) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle(club.getName() + " - Club Details");
        dialogStage.setResizable(false);

        VBox root = new VBox(18);
        root.setPadding(new Insets(30, 35, 30, 35));
        root.setStyle("-fx-background-color: #FFFFFF; -fx-font-family: 'Segoe UI', sans-serif;");

        // Header: Icon + Name
        Node headerIconNode = null;
        String logoPath = null;
        String fallbackEmoji = "🎭";

        if (club.getName().equalsIgnoreCase("Vishaka")) {
            logoPath = "/assets/images/vishaka_logo.png";
            fallbackEmoji = "🌸";
        } else if (club.getName().equalsIgnoreCase("SCRS")) {
            logoPath = "/assets/images/scrs_logo.png";
            fallbackEmoji = "⚙️";
        } else if (club.getName().equalsIgnoreCase("IEEE SMC") || club.getName().contains("SMC")) {
            logoPath = "/assets/images/ieee_smc_logo.png";
            fallbackEmoji = "🤖";
        }

        if (logoPath != null) {
            try {
                InputStream stream = getClass().getResourceAsStream(logoPath);
                if (stream != null) {
                    Image img = new Image(stream);
                    ImageView imgView = new ImageView(img);
                    imgView.setFitWidth(50);
                    imgView.setFitHeight(50);
                    imgView.setPreserveRatio(true);
                    imgView.setSmooth(true);
                    headerIconNode = imgView;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        if (headerIconNode == null) {
            String icon = club.getIcon() != null ? club.getIcon() : fallbackEmoji;
            Label iconLbl = new Label(icon);
            iconLbl.setStyle("-fx-font-size: 36px;");
            headerIconNode = iconLbl;
        }

        VBox modalAvatarBox = new VBox(headerIconNode);
        modalAvatarBox.setStyle("-fx-background-color: #E2E8F0; -fx-padding: 10; -fx-background-radius: 14;");
        modalAvatarBox.setAlignment(Pos.CENTER);

        Label titleLbl = new Label(club.getName());
        titleLbl.getStyleClass().add("login-heading");

        boolean isTech = "Technical".equalsIgnoreCase(club.getCategory());
        Label catBadge = new Label("Category: " + club.getCategory());
        catBadge.getStyleClass().add(isTech ? "club-badge-tech" : "club-badge-nontech");

        VBox headerBox = new VBox(6, titleLbl, catBadge);

        HBox topBox = new HBox(16, modalAvatarBox, headerBox);
        topBox.setAlignment(Pos.CENTER_LEFT);

        // Details content
        Label descTitle = new Label("About the Club");
        descTitle.getStyleClass().add("dashboard-section-title");

        String descriptionStr = club.getDescription();
        if (club.getName().equalsIgnoreCase("SCRS")) {
            descriptionStr = "Encouraging undergraduate and postgraduate students to explore advanced computing domains and publish scientific papers.";
        } else if (club.getName().equalsIgnoreCase("IEEE SMC") || club.getName().contains("SMC")) {
            descriptionStr = "Brainstorming and building real-world application prototypes during collaborative campus marathons.";
        } else if (club.getName().equalsIgnoreCase("Vishaka")) {
            descriptionStr = "The club acts as a creative hub dedicated to blending cultural tradition with innovative entertainment, fostering artistic expression among university students.";
        }
        Label descText = new Label(descriptionStr);
        descText.setStyle("-fx-font-size: 13px; -fx-text-fill: #4A5568; -fx-wrap-text: true;");

        // Additional information grid
        VBox infoBox = new VBox(8);
        infoBox.setStyle("-fx-background-color: #F8FAFC; -fx-padding: 14; -fx-background-radius: 12; -fx-border-color: #E2E8F0; -fx-border-radius: 12;");

        Label leadLbl = new Label("👥 Executive Leads: SAC Faculty & Student Coordinators");
        leadLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #2D3748; -fx-font-weight: bold;");

        Label meetingLbl = new Label("📅 Weekly Sessions: Every Wednesday 4:00 PM - 6:00 PM");
        meetingLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #2D3748;");

        Label statusLbl = new Label("📌 Membership Status: " + (isJoined ? "Active Member" : "Open for Registration"));
        statusLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #2D3748;");

        infoBox.getChildren().addAll(leadLbl, meetingLbl, statusLbl);

        // Buttons
        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("back-button");
        closeBtn.setOnAction(e -> dialogStage.close());

        Button actionBtn;
        if (isJoined) {
            actionBtn = new Button("✓ Joined Member");
            actionBtn.getStyleClass().add("registered-badge");
        } else {
            actionBtn = new Button("Join Club Now");
            actionBtn.getStyleClass().add("login-submit-button");
            actionBtn.setOnAction(e -> {
                try {
                    boolean success = clubDAO.joinClub(userId, club.getClubId());
                    if (success) {
                        AlertUtils.showInfo("Joined Club", "Success", "You have joined " + club.getName() + "!");
                        dialogStage.close();
                        loadClubsView();
                        refreshStatCards();
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            });
        }

        HBox btnBox = new HBox(12, closeBtn, actionBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(topBox, descTitle, descText, infoBox, btnBox);

        Scene scene = new Scene(root, 480, 460);
        if (getScene() != null && !getScene().getStylesheets().isEmpty()) {
            scene.getStylesheets().addAll(getScene().getStylesheets());
        }

        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    private void loadEventsView() {
        mainContentArea.getChildren().clear();

        Label title = new Label("Campus Events Calendar");
        title.getStyleClass().add("login-heading");
        Label sub = new Label("Browse and register for upcoming events");
        sub.getStyleClass().add("login-subheading");

        VBox header = new VBox(4, title, sub);
        VBox upcomingEventsSection = createUpcomingEventsSection();

        mainContentArea.getChildren().addAll(header, upcomingEventsSection);
    }

    private void loadParticipationView() {
        mainContentArea.getChildren().clear();

        Label title = new Label("My Participation & Achievements");
        title.getStyleClass().add("login-heading");
        Label sub = new Label("Your campus activity history and registered events");
        sub.getStyleClass().add("login-subheading");

        VBox header = new VBox(4, title, sub);

        HBox statCards = createStatCardsRow();

        VBox registeredEventsCard = new VBox(16);
        registeredEventsCard.getStyleClass().add("dashboard-section-card");

        Label regTitle = new Label("Registered & Attended Events");
        regTitle.getStyleClass().add("dashboard-section-title");

        VBox eventsList = new VBox(10);
        int userId = studentUser != null ? studentUser.getUserId() : 0;

        try {
            List<Event> userEvents = eventDAO.getUserRegisteredEvents(userId);
            if (userEvents.isEmpty()) {
                Label emptyLbl = new Label("You haven't registered for any events yet.");
                emptyLbl.getStyleClass().add("card-desc");
                eventsList.getChildren().add(emptyLbl);
            } else {
                for (Event event : userEvents) {
                    HBox row = new HBox();
                    row.getStyleClass().add("event-item-card");
                    row.setAlignment(Pos.CENTER_LEFT);

                    Label tLbl = new Label(event.getTitle());
                    tLbl.getStyleClass().add("card-title");

                    Label infoLbl = new Label("📅 " + event.getEventDate() + "  |  📍 " + event.getLocation());
                    infoLbl.getStyleClass().add("card-desc");

                    VBox box = new VBox(4, tLbl, infoLbl);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label statusBadge = new Label("✓ Confirmed Participation");
                    statusBadge.getStyleClass().add("registered-badge");

                    row.getChildren().addAll(box, spacer, statusBadge);
                    eventsList.getChildren().add(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        registeredEventsCard.getChildren().addAll(regTitle, eventsList);
        mainContentArea.getChildren().addAll(header, statCards, registeredEventsCard);
    }

    private void loadChatbotView() {
        mainContentArea.getChildren().clear();

        Label title = new Label("CCMS Campus Assistant Chatbot");
        title.getStyleClass().add("login-heading");
        Label sub = new Label("Ask questions about clubs, events, registrations, or SAC guidelines");
        sub.getStyleClass().add("login-subheading");

        VBox header = new VBox(4, title, sub);

        VBox chatContainer = new VBox(16);
        chatContainer.getStyleClass().add("dashboard-section-card");

        VBox chatMessagesBox = new VBox(12);
        chatMessagesBox.setPadding(new Insets(10));
        chatMessagesBox.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 12; -fx-padding: 16;");
        chatMessagesBox.setMinHeight(300);

        // Welcome message from Chatbot
        Label botWelcome = new Label("🤖 Bot: Hello " + (studentUser != null ? studentUser.getName() : "Student") + "! How can I help you with campus clubs or upcoming events today?");
        botWelcome.setStyle("-fx-background-color: #E2E8F0; -fx-padding: 10 14 10 14; -fx-background-radius: 14; -fx-text-fill: #18243D; -fx-font-size: 13px;");
        chatMessagesBox.getChildren().add(botWelcome);

        TextField inputField = new TextField();
        inputField.setPromptText("Type your message here (e.g., 'What clubs can I join?', 'How to register for events?')...");
        inputField.getStyleClass().add("form-input");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        Button sendBtn = new Button("Send");
        sendBtn.getStyleClass().add("login-submit-button");

        Runnable sendAction = () -> {
            String msg = inputField.getText() != null ? inputField.getText().trim() : "";
            if (msg.isEmpty()) return;

            Label userMsg = new Label("👤 You: " + msg);
            userMsg.setStyle("-fx-background-color: #4F8CFF; -fx-padding: 10 14 10 14; -fx-background-radius: 14; -fx-text-fill: #FFFFFF; -fx-font-size: 13px;");
            chatMessagesBox.getChildren().add(userMsg);

            inputField.clear();

            // Simple intelligent response generation
            String reply;
            String lower = msg.toLowerCase();
            if (lower.contains("club")) {
                reply = "Bot: We have Technical (Coding, Robotics), Cultural, Sports, and Professional (IEEE) clubs available. Check the 'Clubs' tab to view and join!";
            } else if (lower.contains("event") || lower.contains("register")) {
                reply = "Bot: You can register for upcoming events directly from your Student Dashboard under the 'Upcoming Events' section by clicking 'Register'.";
            } else if (lower.contains("password")) {
                reply = "Bot: You can update your account password anytime by clicking 'Change Password' at the bottom of the left sidebar!";
            } else {
                reply = "Bot: I'm here to help! You can ask me about campus clubs, upcoming event registrations, or your participation status.";
            }

            Label botMsg = new Label("🤖 " + reply);
            botMsg.setStyle("-fx-background-color: #E2E8F0; -fx-padding: 10 14 10 14; -fx-background-radius: 14; -fx-text-fill: #18243D; -fx-font-size: 13px;");
            chatMessagesBox.getChildren().add(botMsg);
        };

        sendBtn.setOnAction(e -> sendAction.run());
        inputField.setOnAction(e -> sendAction.run());

        HBox inputRow = new HBox(10, inputField, sendBtn);
        chatContainer.getChildren().addAll(chatMessagesBox, inputRow);

        mainContentArea.getChildren().addAll(header, chatContainer);
    }
}
