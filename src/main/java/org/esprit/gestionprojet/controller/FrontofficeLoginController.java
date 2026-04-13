package org.esprit.gestionprojet.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.esprit.gestionprojet.AppContext;
import org.esprit.gestionprojet.AppRouter;
import org.esprit.gestionprojet.model.User;
import org.esprit.gestionprojet.model.UserRole;

public class FrontofficeLoginController {
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

        User fakeUser = new User("user@static.local", "", "User Demo", UserRole.USER);
        AppContext.sessionService().setCurrentUser(fakeUser);
        AppContext.sessionService().setCurrentUserId(2);
        AppContext.sessionService().setOffice("FRONTOFFICE");
        AppRouter.showFrontHome();
    }

    @FXML
    private void goBack() {
        AppRouter.showLanding();
    }
}
