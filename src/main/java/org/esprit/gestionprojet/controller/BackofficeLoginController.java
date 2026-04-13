package org.esprit.gestionprojet.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.esprit.gestionprojet.AppContext;
import org.esprit.gestionprojet.AppRouter;
import org.esprit.gestionprojet.model.User;
import org.esprit.gestionprojet.model.UserRole;

public class BackofficeLoginController {
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    @FXML
    private void login() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Tous les champs sont obligatoires.");
            return;
        }

        User fakeAdmin = new User("admin@static.local", "", "Admin Demo", UserRole.ADMIN);
        AppContext.sessionService().setCurrentUser(fakeAdmin);
        AppContext.sessionService().setCurrentUserId(1);
        AppContext.sessionService().setOffice("BACKOFFICE");
        AppRouter.showDashboard();
    }

    @FXML
    private void goBack() {
        AppRouter.showLanding();
    }
}
