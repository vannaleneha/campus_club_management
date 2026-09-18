package com.campusclub.view;

import com.campusclub.controller.LoginController;
import com.campusclub.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.util.function.Consumer;

/**
 * LoginView component for the Campus Club Management System.
 * Includes password visibility toggle (eye button) and matches the application visual identity.
 */
public class LoginView extends VBox {

    private final Runnable onBackToHome;
    private final Consumer<User> onLoginSuccess;
    private final LoginController controller;

    private TextField identifierField;
    private PasswordField passwordField;
    private TextField passwordTextField;
    private Button eyeToggleButton;
    private boolean isPasswordVisible = false;

    private ComboBox<String> roleComboBox;
    private Button loginButton;
    private Hyperlink forgotPasswordLink;
    private Button backButton;

    public LoginView(Runnable onBackToHome, Consumer<User> onLoginSuccess) {
        this.onBackToHome = onBackToHome;
        this.onLoginSuccess = onLoginSuccess;
        this.controller = new LoginController();
        initializeUI();
    }

    private void initializeUI() {
        this.getStyleClass().add("login-root-container");
        this.setAlignment(Pos.CENTER);
        this.setPadding(new Insets(30, 40, 40, 40));
        this.setSpacing(20);

        // Top Navigation: Back to Home button
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setMaxWidth(860);

        backButton = new Button("← Back to Home");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(e -> {
            if (onBackToHome != null) {
                onBackToHome.run();
            }
        });
        topBar.getChildren().add(backButton);

        // Main Centered Login Card Container (Left Hero Panel + Right Form Panel)
        HBox loginCard = createLoginCard();

        this.getChildren().addAll(topBar, loginCard);
    }

    /**
     * Creates the split dual-panel login card.
     */
    private HBox createLoginCard() {
        HBox card = new HBox();
        card.getStyleClass().add("login-card");
        card.setMaxWidth(860);
        card.setMinHeight(500);
        card.setAlignment(Pos.CENTER);

        // Left Panel: Hero Information & Branding
        VBox leftPanel = createLeftPanel();
        HBox.setHgrow(leftPanel, Priority.ALWAYS);

        // Right Panel: Form Fields & Actions
        VBox rightPanel = createRightPanel();
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        card.getChildren().addAll(leftPanel, rightPanel);
        return card;
    }

    /**
     * Left side of the card: CampusClub logo, title, tagline, and supporting text.
     */
    private VBox createLeftPanel() {
        VBox left = new VBox(20);
        left.getStyleClass().add("login-card-left");
        left.setAlignment(Pos.CENTER_LEFT);
        left.setPadding(new Insets(45, 40, 45, 40));

        // CampusClub logo/icon
        Label logoLabel = new Label("🎓 CampusClub");
        logoLabel.getStyleClass().add("login-card-logo");

        // Application Title
        Text appTitle = new Text("Campus Club\nManagement System");
        appTitle.getStyleClass().add("login-card-title");

        // Short Tagline
        Text tagline = new Text("Connect  •  Participate  •  Grow");
        tagline.getStyleClass().add("login-card-tagline");

        // Small supporting text
        Label supportingText = new Label("Your gateway to campus clubs, events, and activities.");
        supportingText.getStyleClass().add("login-card-supporting-text");
        supportingText.setWrapText(true);

        // Bullet features
        VBox features = new VBox(10);
        features.setPadding(new Insets(10, 0, 0, 0));
        features.getChildren().addAll(
            createBulletLabel("• Discover campus communities"),
            createBulletLabel("• Stay updated on events & workshops"),
            createBulletLabel("• Network and grow your leadership")
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        left.getChildren().addAll(
            logoLabel,
            appTitle,
            tagline,
            supportingText,
            features,
            spacer
        );

        return left;
    }

    private Label createBulletLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("login-bullet-label");
        return label;
    }

    /**
     * Right side of the card: Welcome header, input fields with eye toggle password field, role dropdown, login button.
     */
    private VBox createRightPanel() {
        VBox right = new VBox(18);
        right.getStyleClass().add("login-card-right");
        right.setAlignment(Pos.CENTER_LEFT);
        right.setPadding(new Insets(45, 45, 45, 45));

        // Heading
        Label heading = new Label("Welcome Back");
        heading.getStyleClass().add("login-heading");

        // Subheading
        Label subheading = new Label("Login to your CampusClub account");
        subheading.getStyleClass().add("login-subheading");

        VBox headerBox = new VBox(4, heading, subheading);

        // Input Field 1: Email / Roll Number
        Label label1 = new Label("Email / Roll Number");
        label1.getStyleClass().add("form-label");

        identifierField = new TextField();
        identifierField.setPromptText("Enter email or roll number");
        identifierField.getStyleClass().add("form-input");

        VBox field1Box = new VBox(6, label1, identifierField);

        // Input Field 2: Password with Eye Toggle Button
        Label label2 = new Label("Password");
        label2.getStyleClass().add("form-label");

        StackPane passwordContainer = createPasswordInputWithToggle();

        VBox field2Box = new VBox(6, label2, passwordContainer);

        // Role Selection
        Label roleLabel = new Label("Select Role");
        roleLabel.getStyleClass().add("form-label");

        roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll("Student", "Coordinator", "Admin");
        roleComboBox.setValue("Student"); // Default selection
        roleComboBox.setMaxWidth(Double.MAX_VALUE);
        roleComboBox.getStyleClass().add("form-combo");

        VBox roleBox = new VBox(6, roleLabel, roleComboBox);

        // Options: Forgot Password Link
        HBox optionsBox = new HBox();
        optionsBox.setAlignment(Pos.CENTER_RIGHT);
        forgotPasswordLink = new Hyperlink("Forgot Password?");
        forgotPasswordLink.getStyleClass().add("forgot-password-link");
        forgotPasswordLink.setOnAction(e -> controller.handleForgotPassword());
        optionsBox.getChildren().add(forgotPasswordLink);

        // Login Button
        loginButton = new Button("Login");
        loginButton.getStyleClass().add("login-submit-button");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(e -> handleLoginSubmit());

        // Enter key shortcuts
        passwordField.setOnAction(e -> handleLoginSubmit());
        passwordTextField.setOnAction(e -> handleLoginSubmit());

        right.getChildren().addAll(
            headerBox,
            field1Box,
            field2Box,
            roleBox,
            optionsBox,
            loginButton
        );

        return right;
    }

    /**
     * Creates a StackPane containing PasswordField, TextField (for unmasked view),
     * and an Eye Toggle Button.
     */
    private StackPane createPasswordInputWithToggle() {
        StackPane container = new StackPane();
        container.setAlignment(Pos.CENTER_RIGHT);

        passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");
        passwordField.getStyleClass().add("form-input");
        passwordField.setPadding(new Insets(10, 45, 10, 14)); // Extra right padding for eye button

        passwordTextField = new TextField();
        passwordTextField.setPromptText("Enter password");
        passwordTextField.getStyleClass().add("form-input");
        passwordTextField.setPadding(new Insets(10, 45, 10, 14));
        passwordTextField.setVisible(false);
        passwordTextField.setManaged(false);

        // Synchronize text bidirectionally between PasswordField and TextField
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!passwordTextField.getText().equals(newVal)) {
                passwordTextField.setText(newVal);
            }
        });

        passwordTextField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!passwordField.getText().equals(newVal)) {
                passwordField.setText(newVal);
            }
        });

        // Eye Toggle Button
        eyeToggleButton = new Button("👁");
        eyeToggleButton.getStyleClass().add("eye-toggle-button");
        eyeToggleButton.setFocusTraversable(false);
        eyeToggleButton.setTooltip(new Tooltip("Show/Hide Password"));

        eyeToggleButton.setOnAction(e -> togglePasswordVisibility());

        container.getChildren().addAll(passwordField, passwordTextField, eyeToggleButton);
        StackPane.setMargin(eyeToggleButton, new Insets(0, 8, 0, 0));

        return container;
    }

    /**
     * Toggles between masked (PasswordField) and unmasked (TextField) password view.
     */
    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;

        if (isPasswordVisible) {
            passwordTextField.setText(passwordField.getText());
            passwordField.setVisible(false);
            passwordField.setManaged(false);

            passwordTextField.setVisible(true);
            passwordTextField.setManaged(true);
            passwordTextField.requestFocus();
            passwordTextField.selectEnd();

            eyeToggleButton.setText("🙈"); // Icon indicating password is text/visible
        } else {
            passwordField.setText(passwordTextField.getText());
            passwordTextField.setVisible(false);
            passwordTextField.setManaged(false);

            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordField.requestFocus();
            passwordField.selectEnd();

            eyeToggleButton.setText("👁"); // Icon indicating password is masked
        }
    }

    private void handleLoginSubmit() {
        String password = isPasswordVisible ? passwordTextField.getText() : passwordField.getText();
        controller.handleLoginWithPassword(identifierField, password, roleComboBox, onLoginSuccess);
    }

    public TextField getIdentifierField() {
        return identifierField;
    }

    public PasswordField getPasswordField() {
        return passwordField;
    }

    public TextField getPasswordTextField() {
        return passwordTextField;
    }

    public Button getEyeToggleButton() {
        return eyeToggleButton;
    }

    public ComboBox<String> getRoleComboBox() {
        return roleComboBox;
    }

    public Button getLoginButton() {
        return loginButton;
    }

    public Hyperlink getForgotPasswordLink() {
        return forgotPasswordLink;
    }

    public Button getBackButton() {
        return backButton;
    }

    /**
     * Clears all login input fields and resets password visibility toggle state to hidden.
     */
    public void clearFields() {
        if (identifierField != null) identifierField.clear();
        if (passwordField != null) passwordField.clear();
        if (passwordTextField != null) passwordTextField.clear();
        if (isPasswordVisible) {
            togglePasswordVisibility();
        }
        if (roleComboBox != null) {
            roleComboBox.setValue("Student");
        }
    }
}
