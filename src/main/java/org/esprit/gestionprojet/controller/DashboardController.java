package org.esprit.gestionprojet.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.esprit.gestionprojet.AppContext;
import org.esprit.gestionprojet.AppRouter;
import org.esprit.gestionprojet.model.User;

public class DashboardController {
    @FXML
    private Label welcomeLabel;
    @FXML
    private Label officeLabel;
    @FXML
    private Button openTachesButton;
    @FXML
    private Button openProjetsButton;

    public void loadSessionData() {
        User currentUser = AppContext.sessionService().getCurrentUser();
        String office = AppContext.sessionService().getOffice();

        if (currentUser == null || office == null) {
            AppRouter.showLanding();
            return;
        }

        welcomeLabel.setText("Welcome, " + currentUser.getFullName());
        officeLabel.setText("Connected to " + office);

        boolean admin = "BACKOFFICE".equalsIgnoreCase(office);
        openProjetsButton.setVisible(admin);
        openProjetsButton.setManaged(admin);
        openTachesButton.setVisible(admin);
        openTachesButton.setManaged(admin);

        if (!admin) {
            AppRouter.showFrontHome();
        }
    }

    @FXML
    private void openProjets() {
        AppRouter.showProjetList();
    }

    @FXML
    private void openTaches() {
        AppRouter.showTacheList();
    }

    @FXML
    private void goDashboard() {
        AppRouter.showDashboard();
    }

    @FXML
    private void goHome() {
        AppRouter.showFrontHome();
    }

    @FXML
    private void logout() {
        AppContext.sessionService().logout();
        AppRouter.showLanding();
    }
}
