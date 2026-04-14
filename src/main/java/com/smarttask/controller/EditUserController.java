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
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class EditUserController implements Initializable {

    @FXML
    private TextField nameField;

    @FXML
    private TextField emailField;

    @FXML
    private ChoiceBox<String> typeChoice;

    @FXML
    private CheckBox enabledCheck;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    private User selectedUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        typeChoice.getItems().setAll("manager", "collaborator");
    }

    public void setUser(User user) {
        this.selectedUser = user;
        nameField.setText(user.getName());
        emailField.setText(user.getEmail());
        typeChoice.setValue(user.getType());
        enabledCheck.setSelected(user.isEnabled());
    }

    @FXML
    private void handleSave(ActionEvent event) {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String type = typeChoice.getValue();
        boolean enabled = enabledCheck.isSelected();

        if (name.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Name and email are required.");
            return;
        }

        if (selectedUser == null) {
            showAlert(Alert.AlertType.ERROR, "Update Failed", "No user selected for update.");
            return;
        }

        selectedUser.setName(name);
        selectedUser.setEmail(email);
        selectedUser.setType(type);
        selectedUser.setEnabled(enabled);

        UserDAO userDAO = new UserDAO();
        if (userDAO.updateUser(selectedUser)) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "User updated successfully!");
            ((Stage) saveButton.getScene().getWindow()).close();
        } else {
            showAlert(Alert.AlertType.ERROR, "Update Failed", "Update failed. Please try again.");
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

