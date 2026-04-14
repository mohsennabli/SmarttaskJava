package com.smarttask.controller;

import com.smarttask.dao.UserDAO;
import com.smarttask.model.User;
import com.smarttask.util.InputValidator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class AddUserController implements Initializable {

    @FXML
    private TextField nameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ChoiceBox<String> typeChoice;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        typeChoice.getItems().setAll("manager", "collaborator");
    }

    @FXML
    private void handleSave(ActionEvent event) {
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
            showAlert(Alert.AlertType.INFORMATION, "Success", "User added successfully!");
            ((Stage) saveButton.getScene().getWindow()).close();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to add user. Please try again.");
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        ((Stage) cancelButton.getScene().getWindow()).close();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

