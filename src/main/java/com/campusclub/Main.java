package com.campusclub;

import com.campusclub.model.User;
import com.campusclub.view.AdminDashboardView;
import com.campusclub.view.CoordinatorDashboardView;
import com.campusclub.view.LoginView;
import com.campusclub.view.StudentDashboardView;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Main application class for Campus Club Management System.
 * Manages navigation between Landing Page, Login Page, Student Dashboard, and Coordinator Dashboard.
 */
public class Main extends Application {

    private Stage primaryStage;
    private Scene scene;
    private BorderPane landingRoot;
    private LoginView loginRoot;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Build Landing Page Layout
        landingRoot = createLandingLayout();

        // Build Login Page Layout (passing navigation callbacks with role-aware dashboard router)
        loginRoot = new LoginView(this::showLandingPage, this::handleUserDashboard);

        // Initial Scene Setup with Landing Page
        scene = new Scene(landingRoot, 1020, 700);

        // Load external CSS stylesheet
        URL cssResource = getClass().getResource("/styles/landing.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        } else {
            System.err.println("Warning: landing.css stylesheet resource not found.");
        }

        primaryStage.setTitle("Campus Club Management System");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    /**
     * Navigates the scene root to the Login Page and clears input fields.
     */
    public void showLoginPage() {
        if (scene != null && loginRoot != null) {
            loginRoot.clearFields();
            scene.setRoot(loginRoot);
        }
    }

    /**
     * Navigates the scene root back to the Landing Page.
     */
    public void showLandingPage() {
        if (scene != null && landingRoot != null) {
            scene.setRoot(landingRoot);
        }
    }

    /**
     * Role-aware router: Navigates to Admin Dashboard for Admin role,
     * Coordinator Dashboard for Coordinator role, or Student Dashboard for Student role.
     */
    public void handleUserDashboard(User user) {
        if (scene != null && user != null) {
            if ("Admin".equalsIgnoreCase(user.getRole())) {
                AdminDashboardView adminView = new AdminDashboardView(user, this::showLoginPage);
                scene.setRoot(adminView);
            } else if ("Coordinator".equalsIgnoreCase(user.getRole())) {
                CoordinatorDashboardView coordinatorView = new CoordinatorDashboardView(user, this::showLoginPage);
                scene.setRoot(coordinatorView);
            } else {
                StudentDashboardView dashboardView = new StudentDashboardView(user, this::showLoginPage);
                scene.setRoot(dashboardView);
            }
        }
    }

    /**
     * Backward-compatible helper method.
     */
    public void showStudentDashboard(User user) {
        handleUserDashboard(user);
    }

    /**
     * Creates the complete Landing Page BorderPane layout.
     */
    private BorderPane createLandingLayout() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        // 1. Top Navigation Header
        HBox navbar = createNavbar();
        root.setTop(navbar);

        // 2. Center Hero Content & Feature Highlights
        VBox centerContent = createHeroContent();
        root.setCenter(centerContent);

        // 3. Bottom Footer
        HBox footer = createFooter();
        root.setBottom(footer);

        return root;
    }

    /**
     * Creates the top navigation bar.
     */
    private HBox createNavbar() {
        HBox navbar = new HBox();
        navbar.getStyleClass().add("navbar");
        navbar.setAlignment(Pos.CENTER_LEFT);
        navbar.setPadding(new Insets(20, 50, 20, 50));

        Label brandLabel = new Label("🎓 CampusClub");
        brandLabel.getStyleClass().add("navbar-brand");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label welcomeBadge = new Label("Student Portal v1.0");
        welcomeBadge.getStyleClass().add("navbar-badge");

        navbar.getChildren().addAll(brandLabel, spacer, welcomeBadge);
        return navbar;
    }

    /**
     * Creates the central hero layout with headline, tagline, description,
     * Get Started button, and feature overview cards.
     */
    private VBox createHeroContent() {
        VBox heroContainer = new VBox(24);
        heroContainer.getStyleClass().add("hero-container");
        heroContainer.setAlignment(Pos.CENTER);
        heroContainer.setPadding(new Insets(30, 50, 40, 50));

        // Campus Portal Pill Tag
        Label portalTag = new Label("WELCOME TO CAMPUS LIFE");
        portalTag.getStyleClass().add("portal-tag");

        // Application Title
        Text appTitle = new Text("Campus Club Management System");
        appTitle.getStyleClass().add("app-title");

        // Tagline
        Text tagline = new Text("Connect  •  Participate  •  Grow");
        tagline.getStyleClass().add("tagline");

        // Short Description
        Label description = new Label(
            "A centralized platform to discover clubs, participate in events, and connect with campus communities."
        );
        description.getStyleClass().add("description");
        description.setWrapText(true);
        description.setMaxWidth(650);

        // Prominent "Get Started" Button (Connected to Login Page Navigation)
        Button getStartedBtn = new Button("Get Started");
        getStartedBtn.getStyleClass().add("get-started-button");
        getStartedBtn.setOnAction(e -> showLoginPage());

        // Action Container
        HBox actionBox = new HBox(getStartedBtn);
        actionBox.setAlignment(Pos.CENTER);
        actionBox.setPadding(new Insets(10, 0, 20, 0));

        // Feature Highlights Section
        HBox featureCards = createFeatureCards();

        heroContainer.getChildren().addAll(
            portalTag,
            appTitle,
            tagline,
            description,
            actionBox,
            featureCards
        );

        return heroContainer;
    }

    /**
     * Creates summary cards showcasing key campus features.
     */
    private HBox createFeatureCards() {
        HBox cardsBox = new HBox(20);
        cardsBox.setAlignment(Pos.CENTER);
        cardsBox.setMaxWidth(850);

        VBox card1 = createCard("🎭 Discover Clubs", "Explore technical, cultural, and sports clubs tailored to your passions.");
        VBox card2 = createCard("📅 Campus Events", "Stay updated on upcoming workshops, hackathons, and cultural fests.");
        VBox card3 = createCard("🚀 Leadership & Growth", "Build teams, manage events, and track your campus involvement.");

        HBox.setHgrow(card1, Priority.ALWAYS);
        HBox.setHgrow(card2, Priority.ALWAYS);
        HBox.setHgrow(card3, Priority.ALWAYS);

        cardsBox.getChildren().addAll(card1, card2, card3);
        return cardsBox;
    }

    private VBox createCard(String title, String desc) {
        VBox card = new VBox(10);
        card.getStyleClass().add("feature-card");
        card.setPadding(new Insets(20));

        Label cardTitle = new Label(title);
        cardTitle.getStyleClass().add("card-title");

        Label cardDesc = new Label(desc);
        cardDesc.getStyleClass().add("card-desc");
        cardDesc.setWrapText(true);

        card.getChildren().addAll(cardTitle, cardDesc);
        return card;
    }

    /**
     * Creates the simple bottom footer bar.
     */
    private HBox createFooter() {
        HBox footer = new HBox();
        footer.getStyleClass().add("footer");
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(15, 30, 20, 30));

        Label footerText = new Label("© 2026 Campus Club Management System  •  Connecting Campus Communities");
        footerText.getStyleClass().add("footer-text");

        footer.getChildren().add(footerText);
        return footer;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
