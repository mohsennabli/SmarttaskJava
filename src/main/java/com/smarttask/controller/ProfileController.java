package com.smarttask.controller;

import com.smarttask.dao.UserDAO;
import com.smarttask.model.User;
import com.smarttask.service.FaceRecognitionService;
import com.smarttask.util.AppSession;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    private static final Path AVATAR_UPLOADS_DIR = Paths.get(
            "src", "main", "resources", "com", "smarttask", "uploads", "avatars"
    );

    @FXML
    private ImageView avatarPreview;

    @FXML
    private Button uploadAvatarButton;

    @FXML
    private Button registerFaceButton;

    @FXML
    private TextField nameField;

    @FXML
    private TextField emailField;

    @FXML
    private ChoiceBox<String> typeChoice;

    @FXML
    private PasswordField currentPasswordField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    private User currentUser;
    private String newAvatarName;
    private final FaceRecognitionService faceRecognitionService = new FaceRecognitionService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        typeChoice.setItems(FXCollections.observableArrayList("manager", "collaborator"));
        applyAvatarCircleClip();

        currentUser = AppSession.getCurrentUser();
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Session Error", "No logged-in user found.");
            closeWindow(saveButton);
            return;
        }

        nameField.setText(currentUser.getName());
        emailField.setText(currentUser.getEmail());
        if (currentUser.getType() != null && !currentUser.getType().isBlank()) {
            typeChoice.setValue(currentUser.getType());
        } else {
            typeChoice.setValue("collaborator");
        }

        if (currentUser.getAvatarName() != null && !currentUser.getAvatarName().isBlank()) {
            Path avatarPath = AVATAR_UPLOADS_DIR.resolve(currentUser.getAvatarName());
            if (Files.exists(avatarPath)) {
                avatarPreview.setImage(new Image(avatarPath.toUri().toString()));
            } else {
                URL classpathAvatar = getClass().getResource(
                        "/com/smarttask/uploads/avatars/" + currentUser.getAvatarName()
                );
                if (classpathAvatar != null) {
                    avatarPreview.setImage(new Image(classpathAvatar.toExternalForm()));
                } else {
                    avatarPreview.setImage(createPlaceholderAvatar());
                }
            }
        } else {
            avatarPreview.setImage(createPlaceholderAvatar());
        }
    }

    @FXML
    private void handleUploadAvatar(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Avatar");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg")
        );

        File selectedFile = chooser.showOpenDialog(uploadAvatarButton.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        String originalName = selectedFile.getName().replaceAll("\\s+", "_");
        String uniqueName = System.currentTimeMillis() + "_" + originalName;
        Path targetPath = AVATAR_UPLOADS_DIR.resolve(uniqueName);

        try {
            Files.createDirectories(AVATAR_UPLOADS_DIR);
            Files.copy(selectedFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            newAvatarName = uniqueName;
            avatarPreview.setImage(new Image(targetPath.toUri().toString()));
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Avatar Error", "Unable to upload avatar image.");
        }
    }

    @FXML
    private void handleRegisterFace(ActionEvent event) {
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Session Error", "No logged-in user found.");
            return;
        }

        if (registerFaceButton != null) {
            registerFaceButton.setDisable(true);
        }

        User userToEnroll = currentUser;
        CompletableFuture
                .supplyAsync(() -> faceRecognitionService.registerFaceForUser(userToEnroll))
                .whenComplete((result, throwable) -> Platform.runLater(() -> {
                    if (registerFaceButton != null) {
                        registerFaceButton.setDisable(false);
                    }

                    if (throwable != null) {
                        showAlert(Alert.AlertType.ERROR, "Face Registration Error", "Unable to register your face.");
                        return;
                    }

                    if (result != null && result.isSuccess()) {
                        if (result.getUser() != null) {
                            currentUser = result.getUser();
                        }
                        AppSession.setCurrentUser(currentUser);
                        showAlert(
                                Alert.AlertType.INFORMATION,
                                "Face Registration",
                                "Votre visage a ete enregistre avec succes."
                        );
                    } else {
                        String message = result != null ? result.getMessage() : "Unknown error";
                        showAlert(Alert.AlertType.ERROR, "Face Registration Error", message);
                    }
                }));
    }

    @FXML
    private void handleSave(ActionEvent event) {
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Session Error", "No logged-in user found.");
            return;
        }

        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String type = typeChoice.getValue();

        if (name.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Name and email are required.");
            return;
        }

        currentUser.setName(name);
        currentUser.setEmail(email);
        currentUser.setType(type == null ? "collaborator" : type);

        String currentPassword = currentPasswordField.getText() == null ? "" : currentPasswordField.getText().trim();
        String newPassword = newPasswordField.getText() == null ? "" : newPasswordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText().trim();

        boolean hasPasswordInput = !currentPassword.isEmpty() || !newPassword.isEmpty() || !confirmPassword.isEmpty();
        if (hasPasswordInput) {
            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "Veuillez remplir tous les champs du mot de passe");
                return;
            }

            if (!BCrypt.checkpw(currentPassword, AppSession.getCurrentUser().getPassword())) {
                showAlert(Alert.AlertType.ERROR, "Password Error", "Mot de passe actuel incorrect");
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "Les nouveaux mots de passe ne correspondent pas");
                return;
            }

            currentUser.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        }

        if (newAvatarName != null) {
            currentUser.setAvatarName(newAvatarName);
        }

        UserDAO userDAO = new UserDAO();
        boolean success = userDAO.updateProfile(currentUser);
        if (success) {
            AppSession.setCurrentUser(currentUser);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Profil mis a jour avec succes !");
            closeWindow(saveButton);
        } else {
            showAlert(Alert.AlertType.ERROR, "Update Error", "Echec de la mise a jour du profil");
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeWindow(cancelButton);
    }

    private void closeWindow(Button button) {
        Stage stage = (Stage) button.getScene().getWindow();
        stage.close();
    }

    private void applyAvatarCircleClip() {
        Circle clip = new Circle(50, 50, 50);
        avatarPreview.setClip(clip);
    }

    private Image createPlaceholderAvatar() {
        WritableImage image = new WritableImage(100, 100);
        PixelWriter writer = image.getPixelWriter();
        Color color = Color.web("#d1d5db");
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 100; x++) {
                writer.setColor(x, y, color);
            }
        }
        return image;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}


