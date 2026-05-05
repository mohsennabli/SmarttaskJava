package com.smarttask.controller;

import com.smarttask.service.ForgotPasswordService;
import com.smarttask.util.InputValidator;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

public class ForgotPasswordController implements Initializable {
    @FXML
    private TextField emailField;

    @FXML
    private TextField tokenField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Button sendResetLinkButton;

    @FXML
    private Button resetPasswordButton;

    @FXML
    private Button backButton;

    @FXML
    private Label statusLabel;

    private final ForgotPasswordService forgotPasswordService = new ForgotPasswordService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        statusLabel.setText("Enter your email address to receive a one-time reset code.");
    }

    @FXML
    private void handleSendResetLink(ActionEvent event) {
        String email = InputValidator.sanitize(emailField.getText());
        if (email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter your email address.");
            return;
        }
        if (!InputValidator.isValidEmail(email)) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter a valid email address.");
            return;
        }

        sendResetLinkButton.setDisable(true);
        statusLabel.setText("Sending reset instructions...");

        CompletableFuture.supplyAsync(() -> forgotPasswordService.requestPasswordReset(email))
                .whenComplete((success, throwable) -> Platform.runLater(() -> {
                    sendResetLinkButton.setDisable(false);

                    if (throwable != null) {
                        statusLabel.setText("Unable to send the reset email.");
                        showAlert(Alert.AlertType.ERROR, "Reset Failed", "Unable to send the password reset email. Please try again later.");
                        return;
                    }

                    if (Boolean.TRUE.equals(success)) {
                        statusLabel.setText("If your email exists, a password reset link has been sent.");
                        showAlert(Alert.AlertType.INFORMATION, "Reset Email Sent",
                                "If your email exists, a password reset link has been sent.");
                    } else {
                        statusLabel.setText("Unable to send the reset email.");
                        showAlert(Alert.AlertType.ERROR, "Reset Failed",
                                "Unable to send the password reset email. Please verify the email service configuration in your .env file.");
                    }
                }));
    }

    @FXML
    private void handleResetPassword(ActionEvent event) {
        String email = InputValidator.sanitize(emailField.getText());
        String token = InputValidator.sanitize(tokenField.getText());
        String newPassword = InputValidator.sanitize(newPasswordField.getText());
        String confirmPassword = InputValidator.sanitize(confirmPasswordField.getText());

        if (email.isEmpty() || token.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please fill in all reset fields.");
            return;
        }
        if (!InputValidator.isValidEmail(email)) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter a valid email address.");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "The new passwords do not match.");
            return;
        }
        if (!InputValidator.isValidPassword(newPassword)) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "Password must be at least 8 characters and include letters and numbers.");
            return;
        }

        resetPasswordButton.setDisable(true);
        statusLabel.setText("Verifying reset code...");

        CompletableFuture.supplyAsync(() -> forgotPasswordService.resetPassword(email, token, newPassword))
                .whenComplete((success, throwable) -> Platform.runLater(() -> {
                    resetPasswordButton.setDisable(false);

                    if (throwable != null) {
                        statusLabel.setText("Unable to reset the password.");
                        showAlert(Alert.AlertType.ERROR, "Reset Failed", "Unable to reset the password. Please try again.");
                        return;
                    }

                    if (Boolean.TRUE.equals(success)) {
                        statusLabel.setText("Password updated successfully.");
                        showAlert(Alert.AlertType.INFORMATION, "Password Reset Successful",
                                "Your password has been updated successfully. You can now log in.");
                        closeWindow();
                    } else {
                        statusLabel.setText("Invalid or expired reset code.");
                        showAlert(Alert.AlertType.ERROR, "Reset Failed",
                                "Invalid or expired reset code. Please request a new one.");
                    }
                }));
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) backButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

