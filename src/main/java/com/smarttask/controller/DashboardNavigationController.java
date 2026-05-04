package com.smarttask.controller;

import com.smarttask.model.User;
import com.smarttask.util.AppSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

@SuppressWarnings("unused")
public abstract class DashboardNavigationController {

    @FXML
    protected Label currentUserLabel;

    @FXML
    protected Button formationsNavBtn;

    @FXML
    protected Button statsNavBtn;

    protected void initializeDashboardHeader() {
        User currentUser = AppSession.getCurrentUser();
        if (currentUserLabel != null) {
            currentUserLabel.setText(currentUser != null ? currentUser.getName() : "");
        }

        boolean isManager = isManager();
        if (formationsNavBtn != null) {
            formationsNavBtn.setVisible(isManager);
            formationsNavBtn.setManaged(isManager);
        }
        if (statsNavBtn != null) {
            statsNavBtn.setVisible(isManager);
            statsNavBtn.setManaged(isManager);
        }
    }

    protected boolean isManager() {
        User currentUser = AppSession.getCurrentUser();
        return currentUser != null && "manager".equalsIgnoreCase(currentUser.getType());
    }

    protected Stage currentStage() {
        if (currentUserLabel != null && currentUserLabel.getScene() != null) {
            return (Stage) currentUserLabel.getScene().getWindow();
        }
        throw new IllegalStateException("Unable to resolve current stage.");
    }

    protected void navigateTo(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = currentStage();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            System.err.println("Navigation error: " + e.getMessage());
        }
    }

    protected void openProfileModal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smarttask/profile.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Mon Profil");
            stage.initModality(Modality.APPLICATION_MODAL);
            if (currentUserLabel != null && currentUserLabel.getScene() != null) {
                stage.initOwner(currentUserLabel.getScene().getWindow());
            }
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            System.err.println("Navigation error: " + e.getMessage());
        }
    }

    @FXML
    protected void handleGoToUsers() {
        navigateTo("/com/smarttask/users.fxml");
    }

    @FXML
    protected void handleGoToProjets() {
        navigateTo("/com/smarttask/projet-list.fxml");
    }

    @FXML
    protected void handleGoToTaches() {
        navigateTo("/com/smarttask/tache-list.fxml");
    }

    @FXML
    protected void handleGoToFormations() {
        navigateTo("/com/smarttask/formations.fxml");
    }

    @FXML
    protected void handleGoToMyEnrollments() {
        navigateTo("/com/smarttask/my-enrollments.fxml");
    }

    @FXML
    protected void handleGoToTickets() {
        navigateTo("/com/smarttask/ticket.fxml");
    }

    @FXML
    protected void handleGoToCalendar() {
        navigateTo("/com/smarttask/calendar.fxml");
    }

    @FXML
    protected void handleGoToStatistiques() {
        navigateTo("/com/smarttask/statistiques.fxml");
    }

    @FXML
    protected void handleGoToChatbot() {
        navigateTo("/com/smarttask/chatbot.fxml");
    }

    @FXML
    protected void handleProfile() {
        openProfileModal();
    }

    @FXML
    protected void handleLogout() {
        AppSession.clear();
        navigateTo("/com/smarttask/login.fxml");
    }
}


