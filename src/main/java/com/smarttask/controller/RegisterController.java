package com.smarttask.controller;

import com.smarttask.dao.UserDAO;
import com.smarttask.model.User;
import com.smarttask.util.InputValidator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {

    @FXML
    private TextField nameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ChoiceBox<String> typeChoice;

    @FXML
    private Button registerButton;

    @FXML
    private Hyperlink loginLink;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        typeChoice.getItems().addAll("manager", "collaborator");
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        String name = InputValidator.sanitize(nameField.getText());
        String email = InputValidator.sanitize(emailField.getText());
        String password = InputValidator.sanitize(passwordField.getText());
        String type = InputValidator.sanitize(typeChoice.getValue());

        String validationMessage = InputValidator.validateUserCreationFields(name, email, password, type);
        if (validationMessage != null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", validationMessage);
            return;
        }

        UserDAO userDAO = new UserDAO();
        if (userDAO.emailExists(email)) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Email already registered.");
            return;
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        user.setType(type);
        user.setRoles("[]");
        user.setEnabled(true);

        if (userDAO.register(user)) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Account created successfully!");
            // Clear form
            nameField.clear();
            emailField.clear();
            passwordField.clear();
            typeChoice.setValue(null);
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Registration failed. Please try again.");
        }
    }

    @FXML
    private void handleLoginLink(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smarttask/login.fxml"));
            Stage stage = (Stage) loginLink.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to open login screen.");
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



