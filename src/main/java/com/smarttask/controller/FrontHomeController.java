package com.smarttask.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import com.smarttask.AppContext;
import com.smarttask.AppRouter;

public class FrontHomeController {
    @FXML
    private Button dashboardButton;

    @FXML
    private void initialize() {
        boolean admin = "BACKOFFICE".equalsIgnoreCase(AppContext.sessionService().getOffice());
        if (dashboardButton != null) {
            dashboardButton.setVisible(admin);
            dashboardButton.setManaged(admin);
        }
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
    private void goProjects() {
        AppRouter.showProjetList();
    }

    @FXML
    private void goTaches() {
        AppRouter.showTacheList();
    }

    @FXML
    private void logout() {
        AppContext.sessionService().logout();
        AppRouter.showLanding();
    }
}