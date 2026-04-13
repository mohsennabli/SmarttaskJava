package org.esprit.gestionprojet.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.esprit.gestionprojet.AppContext;
import org.esprit.gestionprojet.AppRouter;
import org.esprit.gestionprojet.model.User;
import org.esprit.gestionprojet.model.UserRole;

public class LandingController {
    @FXML
    private Label subtitleLabel;

    @FXML
    private void goAsAdmin() {
        User fakeAdmin = new User("admin@static.local", "", "Admin Demo", UserRole.ADMIN);
        AppContext.sessionService().setCurrentUser(fakeAdmin);
        AppContext.sessionService().setCurrentUserId(1);
        AppContext.sessionService().setOffice("BACKOFFICE");
        AppRouter.showDashboard();
    }

    @FXML
    private void goAsNormalUser() {
        User fakeUser = new User("user@static.local", "", "User Demo", UserRole.USER);
        AppContext.sessionService().setCurrentUser(fakeUser);
        AppContext.sessionService().setCurrentUserId(2);
        AppContext.sessionService().setOffice("FRONTOFFICE");
        AppRouter.showFrontHome();
    }
}
