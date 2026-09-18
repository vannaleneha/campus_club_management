package com.campusclub.controller;

import com.campusclub.dao.UserDAO;
import com.campusclub.model.User;
import com.campusclub.util.AlertUtils;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.SQLException;
import java.util.function.Consumer;

/**
 * Controller connecting Login UI fields to MySQL database authentication via UserDAO.
 */
public class LoginController {

    private final UserDAO userDAO;

    public LoginController() {
        this.userDAO = new UserDAO();
    }

    /**
     * Handles login authentication reading password directly.
     */
    public void handleLoginWithPassword(TextField identifierField, String password, ComboBox<String> roleComboBox, Consumer<User> onLoginSuccess) {
        String identifier = identifierField.getText() != null ? identifierField.getText().trim() : "";
        String role = roleComboBox.getValue();
        String pwd = password != null ? password.trim() : "";

        // 1. Validate that no field is empty
        if (identifier.isEmpty() || pwd.isEmpty() || role == null || role.trim().isEmpty()) {
            AlertUtils.showWarning(
                "Validation Error",
                "Empty Fields",
                "Please fill in all fields (Email / Roll Number, Password, and Role) before logging in."
            );
            return;
        }

        // 2. Database authentication via UserDAO
        try {
            User authenticatedUser = userDAO.authenticateUser(identifier, pwd, role);

            if (authenticatedUser != null) {
                // Successful login message showing dynamic user's name from DB
                // Successful login - proceed to dashboard without popup

                // Navigate to Student Dashboard
                if (onLoginSuccess != null) {
                    onLoginSuccess.accept(authenticatedUser);
                }
            } else {
                // Invalid credentials error message
                AlertUtils.showError(
                    "Login Failed",
                    "Invalid Credentials",
                    "Invalid email/roll number, password, or role selection. Please verify your details."
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.toString();
            AlertUtils.showError(
                "Database Error",
                "Database Connection Error",
                "Could not connect to MySQL ('campus_club').\n\n" +
                "Error details:\n" + errorMsg
            );
        }
    }

    /**
     * Legacy handler method taking PasswordField directly.
     */
    public void handleLogin(TextField identifierField, PasswordField passwordField, ComboBox<String> roleComboBox, Consumer<User> onLoginSuccess) {
        String pwd = passwordField.getText() != null ? passwordField.getText() : "";
        handleLoginWithPassword(identifierField, pwd, roleComboBox, onLoginSuccess);
    }

    /**
     * Handles "Forgot Password?" hyperlink click.
     */
    public void handleForgotPassword() {
        AlertUtils.showInfo(
            "Forgot Password",
            "Password Recovery",
            "Please contact your Campus Administrator or SAC Coordinator to reset your account password."
        );
    }
}
