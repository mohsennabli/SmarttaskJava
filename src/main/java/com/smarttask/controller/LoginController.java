package com.smarttask.controller;

import com.smarttask.dao.UserDAO;
import com.smarttask.model.User;
import com.smarttask.util.AppSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Hyperlink registerLink;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Intentionally left empty for now.
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please fill in all fields.");
            return;
        }

        UserDAO userDAO = new UserDAO();
        User user = userDAO.login(email, password);
        if (user != null) {
            AppSession.setCurrentUser(user);
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smarttask/users.fxml"));
                Stage stage = (Stage) loginButton.getScene().getWindow();
                stage.setScene(new Scene(loader.load()));
                stage.setMaximized(true);
                stage.show();
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to open users screen.");
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid credentials or account disabled.");
        }
    }

    @FXML
    private void handleRegisterLink(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smarttask/register.fxml"));
            Stage stage = (Stage) registerLink.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to open register screen.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

