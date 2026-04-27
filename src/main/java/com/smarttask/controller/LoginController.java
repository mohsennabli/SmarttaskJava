package com.smarttask.controller;

import com.smarttask.dao.UserDAO;
import com.smarttask.model.User;
import com.smarttask.service.GitHubOAuthService;
import com.smarttask.service.GitHubOAuthService.GitHubUserInfo;
import com.smarttask.service.GoogleOAuthService;
import com.smarttask.service.GoogleOAuthService.GoogleUserInfo;
import com.smarttask.util.AppSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.application.Platform;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Button googleSignInButton;

    @FXML
    private Button githubSignInButton;

    @FXML
    private Hyperlink registerLink;

    private final UserDAO userDAO = new UserDAO();
    private final GoogleOAuthService googleOAuthService = new GoogleOAuthService();
    private final GitHubOAuthService githubOAuthService = new GitHubOAuthService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Intentionally left empty for now.
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please fill in all fields.");
            return;
        }

        User user = userDAO.login(email, password);
        if (user != null) {
            AppSession.setCurrentUser(user);
            try {
                openUsersView();
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to open users screen.");
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid credentials or account disabled.");
        }
    }

    @FXML
    private void handleRegisterLink(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smarttask/register.fxml"));
            Stage stage = (Stage) registerLink.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to open register screen.");
        }
    }

    @FXML
    private void handleGoogleSignIn(ActionEvent event) {
        if (!GoogleOAuthService.isConfigurationReady()) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "Google OAuth Not Configured",
                    "Please set SMARTTASK_GOOGLE_CLIENT_ID, SMARTTASK_GOOGLE_CLIENT_SECRET and SMARTTASK_GOOGLE_REDIRECT_URI."
            );
            return;
        }

        String expectedState = googleOAuthService.generateState();
        try {
            googleOAuthService.startLocalOAuthEndpoints(expectedState);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "OAuth Error", "Unable to start local OAuth callback endpoint.");
            return;
        }

        String authorizationUrl;
        try {
            authorizationUrl = googleOAuthService.fetchLocalAuthorizationUrl();
        } catch (IOException | InterruptedException e) {
            googleOAuthService.stopLocalOAuthEndpoints();
            showAlert(Alert.AlertType.ERROR, "OAuth Error", "Unable to generate Google authorization URL.");
            return;
        }

        Stage oauthStage = buildOAuthStage("Sign in with Google");
        WebView webView = (WebView) ((VBox) oauthStage.getScene().getRoot()).getChildren().get(0);
        WebEngine webEngine = webView.getEngine();

        CompletableFuture<GoogleOAuthService.OAuthCallbackData> callbackFuture = googleOAuthService.getCallbackFuture();
        callbackFuture.whenComplete((callback, throwable) -> Platform.runLater(() -> {
            googleOAuthService.stopLocalOAuthEndpoints();
            if (oauthStage.isShowing()) {
                oauthStage.close();
            }

            if (throwable != null) {
                showAlert(Alert.AlertType.ERROR, "Google Sign-In Failed", "Google authentication was canceled or failed.");
                return;
            }

            handleGoogleCallback(callback.getCode(), callback.getState(), expectedState);
        }));

        oauthStage.setOnHidden(e -> googleOAuthService.stopLocalOAuthEndpoints());
        webEngine.load(authorizationUrl);
        oauthStage.show();
    }

    @FXML
    private void handleGitHubSignIn(ActionEvent event) {
        if (!GitHubOAuthService.isConfigurationReady()) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "GitHub OAuth Not Configured",
                    "Please set SMARTTASK_GITHUB_CLIENT_ID, SMARTTASK_GITHUB_CLIENT_SECRET and SMARTTASK_GITHUB_REDIRECT_URI."
            );
            return;
        }

        String expectedState = githubOAuthService.generateState();
        try {
            githubOAuthService.startLocalOAuthEndpoints(expectedState);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "OAuth Error", "Unable to start local GitHub callback endpoint.");
            return;
        }

        String authorizationUrl;
        try {
            authorizationUrl = githubOAuthService.fetchLocalAuthorizationUrl();
        } catch (IOException | InterruptedException e) {
            githubOAuthService.stopLocalOAuthEndpoints();
            showAlert(Alert.AlertType.ERROR, "OAuth Error", "Unable to generate GitHub authorization URL.");
            return;
        }

        Stage oauthStage = buildOAuthStage("Sign in with GitHub");
        WebView webView = (WebView) ((VBox) oauthStage.getScene().getRoot()).getChildren().get(0);
        WebEngine webEngine = webView.getEngine();

        CompletableFuture<GitHubOAuthService.OAuthCallbackData> callbackFuture = githubOAuthService.getCallbackFuture();
        callbackFuture.whenComplete((callback, throwable) -> Platform.runLater(() -> {
            githubOAuthService.stopLocalOAuthEndpoints();
            if (oauthStage.isShowing()) {
                oauthStage.close();
            }

            if (throwable != null) {
                showAlert(Alert.AlertType.ERROR, "GitHub Sign-In Failed", "GitHub authentication was canceled or failed.");
                return;
            }

            handleGitHubCallback(callback.getCode(), callback.getState(), expectedState);
        }));

        oauthStage.setOnHidden(e -> githubOAuthService.stopLocalOAuthEndpoints());
        webEngine.load(authorizationUrl);
        oauthStage.show();
    }

    private void handleGoogleCallback(String code, String returnedState, String expectedState) {
        if (!expectedState.equals(returnedState)) {
            showAlert(Alert.AlertType.ERROR, "Security Error", "Invalid OAuth state received.");
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                GoogleUserInfo googleUser = googleOAuthService.authenticateWithCode(code);
                User user = userDAO.upsertGoogleUser(googleUser.getSub(), googleUser.getEmail(), googleUser.getName());
                if (user == null) {
                    throw new IOException("Unable to link or create user account.");
                }
                return user;
            } catch (IOException e) {
                throw new CompletionException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CompletionException(e);
            }
        }).whenComplete((user, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                showAlert(Alert.AlertType.ERROR, "Google Sign-In Failed", "Unable to complete Google authentication.");
                return;
            }

            if (!user.isEnabled()) {
                showAlert(Alert.AlertType.ERROR, "Account Disabled", "Your account is disabled.");
                return;
            }

            AppSession.setCurrentUser(user);
            try {
                openUsersView();
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to open users screen.");
            }
        }));
    }

    private void handleGitHubCallback(String code, String returnedState, String expectedState) {
        if (!expectedState.equals(returnedState)) {
            showAlert(Alert.AlertType.ERROR, "Security Error", "Invalid OAuth state received.");
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                GitHubUserInfo githubUser = githubOAuthService.authenticateWithCode(code);
                User user = userDAO.upsertGitHubUser(githubUser.getId(), githubUser.getEmail(), githubUser.getName());
                if (user == null) {
                    throw new IOException("Unable to link or create user account.");
                }
                return user;
            } catch (IOException e) {
                throw new CompletionException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CompletionException(e);
            }
        }).whenComplete((user, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                showAlert(Alert.AlertType.ERROR, "GitHub Sign-In Failed", "Unable to complete GitHub authentication.");
                return;
            }

            if (!user.isEnabled()) {
                showAlert(Alert.AlertType.ERROR, "Account Disabled", "Your account is disabled.");
                return;
            }

            AppSession.setCurrentUser(user);
            try {
                openUsersView();
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to open users screen.");
            }
        }));
    }

    private Stage buildOAuthStage(String title) {
        WebView webView = new WebView();
        webView.setPrefSize(900, 700);

        Button closeButton = new Button("Cancel");
        closeButton.getStyleClass().add("secondary-button");

        VBox root = new VBox(10, webView, closeButton);
        root.setPadding(new Insets(10));
        root.setAlignment(Pos.CENTER);

        Stage ownerStage = (Stage) loginButton.getScene().getWindow();
        Stage oauthStage = new Stage();
        oauthStage.setTitle(title);
        oauthStage.initOwner(ownerStage);
        oauthStage.initModality(Modality.APPLICATION_MODAL);
        oauthStage.setScene(new Scene(root, 920, 760));

        closeButton.setOnAction(e -> oauthStage.close());
        return oauthStage;
    }

    private void openUsersView() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smarttask/users.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) loginButton.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setMaximized(true);
        stage.show();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

