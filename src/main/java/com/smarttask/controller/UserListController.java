package com.smarttask.controller;

import com.smarttask.dao.UserDAO;
import com.smarttask.model.User;
import com.smarttask.util.AppSession;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class UserListController implements Initializable {

    @FXML
    private TableView<User> usersTable;

    @FXML
    private TableColumn<User, String> colName;

    @FXML
    private TableColumn<User, String> colEmail;

    @FXML
    private TableColumn<User, String> colType;

    @FXML
    private TableColumn<User, Boolean> colEnabled;

    @FXML
    private Button addButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button enableButton;

    @FXML
    private Button disableButton;

    @FXML
    private Button logoutButton;

    @FXML
    private Button profileButton;

    @FXML
    private TextField searchField;

    @FXML
    private Button searchButton;

    @FXML
    private Button resetButton;

    @FXML
    private Label statsLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colEnabled.setCellValueFactory(new PropertyValueFactory<>("enabled"));
        colName.setSortable(true);
        colEmail.setSortable(true);
        colType.setSortable(true);

        colEnabled.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                if (value) {
                    setText("✓ Active");
                    setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold;");
                } else {
                    setText("✗ Disabled");
                    setStyle("-fx-text-fill: #b91c1c; -fx-font-weight: bold;");
                }
            }
        });

        loadUsers();
    }

    @FXML
    private void handleAdd(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smarttask/add-user.fxml"));
            Stage addStage = new Stage();
            addStage.setTitle("Add User");
            addStage.initModality(Modality.APPLICATION_MODAL);
            addStage.setScene(new Scene(loader.load()));
            addStage.setMaximized(true);

            addStage.showAndWait();
            loadUsers();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to open add user screen.");
        }
    }

    @FXML
    private void handleEdit(ActionEvent event) {
        User selectedUser = usersTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select a user to edit.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smarttask/edit-user.fxml"));
            Stage editStage = new Stage();
            editStage.setTitle("Edit User");
            editStage.initModality(Modality.APPLICATION_MODAL);
            editStage.setScene(new Scene(loader.load()));
            editStage.setMaximized(true);

            EditUserController editController = loader.getController();
            editController.setUser(selectedUser);

            editStage.showAndWait();
            loadUsers();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to open edit user screen.");
        }
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        User selectedUser = usersTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select a user to delete.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Delete");
        confirmation.setHeaderText(null);
        confirmation.setContentText("Are you sure you want to delete " + selectedUser.getName() + "?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            UserDAO userDAO = new UserDAO();
            if (userDAO.deleteUser(selectedUser.getIduser())) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "User deleted successfully!");
                loadUsers();
            } else {
                showAlert(Alert.AlertType.ERROR, "Delete Failed", "Delete failed. Please try again.");
            }
        }
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim();
        if (keyword.isEmpty()) {
            loadUsers();
            return;
        }

        UserDAO userDAO = new UserDAO();
        List<User> users = userDAO.searchUsers(keyword);
        setUsersTableData(users);
    }

    @FXML
    private void handleReset(ActionEvent event) {
        searchField.clear();
        loadUsers();
    }

    @FXML
    private void handleEnable(ActionEvent event) {
        User selectedUser = usersTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select a user.");
            return;
        }
        if (selectedUser.isEnabled()) {
            showAlert(Alert.AlertType.WARNING, "Already Enabled", "User is already enabled.");
            return;
        }

        UserDAO userDAO = new UserDAO();
        if (userDAO.toggleUserStatus(selectedUser.getIduser(), true)) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "User enabled successfully!");
            loadUsers();
        } else {
            showAlert(Alert.AlertType.ERROR, "Update Failed", "Unable to enable user. Please try again.");
        }
    }

    @FXML
    private void handleDisable(ActionEvent event) {
        User selectedUser = usersTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select a user.");
            return;
        }
        if (!selectedUser.isEnabled()) {
            showAlert(Alert.AlertType.WARNING, "Already Disabled", "User is already disabled.");
            return;
        }

        UserDAO userDAO = new UserDAO();
        if (userDAO.toggleUserStatus(selectedUser.getIduser(), false)) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "User disabled successfully!");
            loadUsers();
        } else {
            showAlert(Alert.AlertType.ERROR, "Update Failed", "Unable to disable user. Please try again.");
        }
    }


    @FXML
    private void handleLogout(ActionEvent event) {
        AppSession.clear();
        usersTable.getItems().clear();
        usersTable.getSelectionModel().clearSelection();
        searchField.clear();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smarttask/login.fxml"));
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to return to login screen.");
        }
    }

    @FXML
    private void handleProfile(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smarttask/profile.fxml"));
            Stage profileStage = new Stage();
            profileStage.setTitle("Mon Profil");
            profileStage.initModality(Modality.APPLICATION_MODAL);
            profileStage.initOwner(profileButton.getScene().getWindow());
            profileStage.setScene(new Scene(loader.load()));
            profileStage.setResizable(false);

            profileStage.showAndWait();
            loadUsers();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to open profile screen.");
        }
    }

    private void loadUsers() {
        UserDAO userDAO = new UserDAO();
        List<User> users = userDAO.getAllUsers();
        setUsersTableData(users);
    }

    private void setUsersTableData(List<User> users) {
        ObservableList<User> data = FXCollections.observableArrayList(users);
        usersTable.setItems(data);
        updateStats(users);

        colName.setSortType(TableColumn.SortType.ASCENDING);
        usersTable.getSortOrder().clear();
        usersTable.getSortOrder().add(colName);
        usersTable.sort();
    }

    private void updateStats(List<User> users) {
        int total = users.size();
        long enabled = users.stream().filter(User::isEnabled).count();
        long disabled = total - enabled;
        statsLabel.setText("Total users: " + total + "  |  Enabled: " + enabled + "  |  Disabled: " + disabled);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

