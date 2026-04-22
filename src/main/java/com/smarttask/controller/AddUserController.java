package com.smarttask.controller;

import com.smarttask.dao.UserDAO;
import com.smarttask.model.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
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
    private CheckBox enabledCheck;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        typeChoice.getItems().setAll("manager", "collaborator");
        enabledCheck.setSelected(true);
    }

    @FXML
    private void handleSave(ActionEvent event) {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText().trim();
        String type = typeChoice.getValue() == null ? "" : typeChoice.getValue().trim();
        boolean enabled = enabledCheck.isSelected();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please fill in all required fields.");
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
        user.setEnabled(enabled);

        if (userDAO.register(user)) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "User added successfully!");
            ((Stage) saveButton.getScene().getWindow()).close();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to add user. Please try again.");
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

