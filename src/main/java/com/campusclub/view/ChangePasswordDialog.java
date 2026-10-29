package com.campusclub.view;

import com.campusclub.dao.UserDAO;
import com.campusclub.model.User;
import com.campusclub.util.AlertUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.SQLException;

/**
 * Reusable modal dialog for changing user passwords across all roles.
 * Supports real-time text synchronization, show/hide password toggle,
 * input validation, and secure database updates via UserDAO.
 */
public class ChangePasswordDialog {

    private final User user;
    private final UserDAO userDAO;

    public ChangePasswordDialog(User user) {
        this(user, new UserDAO());
    }

    public ChangePasswordDialog(User user, UserDAO userDAO) {
        this.user = user;
        this.userDAO = userDAO;
    }

    public void showAndWait(Stage ownerStage) {
        if (user == null) return;

        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        if (ownerStage != null) {
            dialogStage.initOwner(ownerStage);
        }
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

            // Validation 3: Verify current password against database
            try {
                boolean isCurrentValid = userDAO.verifyPassword(user.getUserId(), currentPwd.trim());
                if (!isCurrentValid) {
                    AlertUtils.showError(
                        "Validation Error",
                        "Incorrect Current Password",
                        "The current password you entered is incorrect."
                    );
                    return;
                }

                // Execute Update in database
                boolean updated = userDAO.updatePassword(user.getUserId(), newPwd.trim());
                if (updated) {
                    user.setPassword(newPwd.trim());

                    AlertUtils.showInfo(
                        "Success",
                        "Password Changed",
                        "Your password has been successfully updated!"
                    );

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
        if (ownerStage != null && ownerStage.getScene() != null && !ownerStage.getScene().getStylesheets().isEmpty()) {
            dialogScene.getStylesheets().addAll(ownerStage.getScene().getStylesheets());
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
}
