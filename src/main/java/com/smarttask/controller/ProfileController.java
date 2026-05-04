package com.smarttask.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smarttask.dao.UserDAO;
import com.smarttask.model.User;
import com.smarttask.util.AppSession;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
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
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

public class ProfileController implements Initializable {

    private static final Gson GSON = new Gson();
    private static final String PYTHON_EXECUTABLE = "/home/mohsen-nabli/IdeaProjects/smarttask-javafx/face_env/bin/python3";
    private static final Path FACE_REGISTER_SCRIPT = Paths.get("/home/mohsen-nabli/IdeaProjects/smarttask-javafx/face_register.py").toAbsolutePath().normalize();
    private static final Path AVATAR_UPLOADS_DIR = Paths.get(
            "src", "main", "resources", "com", "smarttask", "uploads", "avatars"
    );

    @FXML
    private ImageView avatarPreview;

    @FXML
    private Button uploadAvatarButton;

    @FXML
    private Button registerFaceBtn;

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
    private void handleRegisterFace(ActionEvent event) {
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Session Error", "No logged-in user found.");
            return;
        }

        showAlert(
                Alert.AlertType.INFORMATION,
                "Face Registration",
                "Position your face clearly in front of the camera.\n" +
                        "Only one face must be visible. The camera will activate for 10 seconds."
        );

        registerFaceBtn.setDisable(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                UserDAO userDAO = new UserDAO();
                ProcessBuilder processBuilder = new ProcessBuilder(
                        PYTHON_EXECUTABLE,
                        FACE_REGISTER_SCRIPT.toString(),
                        String.valueOf(currentUser.getIduser())
                );
                processBuilder.redirectErrorStream(true);

                try {
                    System.out.println("[DEBUG] Starting face registration process: " + String.join(" ", processBuilder.command()));
                    
                    Process process = processBuilder.start();
                    
                    // Read output in a separate thread to avoid deadlock
                    String output = readProcessOutput(process.getInputStream());
                    
                    // Wait for process with timeout (30 seconds should be enough for 10s capture + overhead)
                    boolean completed = process.waitFor(30, TimeUnit.SECONDS);
                    if (!completed) {
                        process.destroyForcibly();
                        throw new IOException("Le script Python a dépassé le délai autorisé (timeout 30s).");
                    }
                    
                    System.out.println("[DEBUG] Process completed with output length: " + output.length());
                    System.out.println("[DEBUG] Process exit code: " + process.exitValue());

                    if (output.isBlank()) {
                        throw new IOException("Le script Python n'a renvoye aucune sortie.");
                    }

                    JsonObject response = JsonParser.parseString(output).getAsJsonObject();
                    boolean success = response.has("success") && response.get("success").getAsBoolean();
                    if (!success) {
                        String message = response.has("message") ? response.get("message").getAsString() : "Face registration failed.";
                        throw new IOException(message);
                    }

                    JsonArray embedding = response.getAsJsonArray("embedding");
                    if (embedding == null || embedding.isEmpty()) {
                        throw new IOException("Face embedding is missing in the Python response.");
                    }

                    String embeddingJson = GSON.toJson(embedding);
                    boolean saved = userDAO.saveFaceEmbedding(currentUser.getIduser(), embeddingJson);
                    if (!saved) {
                        throw new IOException("Failed to save face embedding in the database.");
                    }

                    AppSession.getCurrentUser().setFaceEmbedding(embeddingJson);
                    Platform.runLater(() -> showAlert(Alert.AlertType.INFORMATION, "Success", "Face registered successfully!"));
                } catch (Exception e) {
                    System.err.println("[ERROR] Face registration error: " + e.getMessage());
                    e.printStackTrace();
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Face Registration Error", e.getMessage()));
                } finally {
                    Platform.runLater(() -> registerFaceBtn.setDisable(false));
                }

                return null;
            }
        };

        Thread thread = new Thread(task, "smarttask-face-register");
        thread.setDaemon(true);
        thread.start();
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

    private String readProcessOutput(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String line;
        String jsonLine = null;
        StringBuilder all = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            // Log python output for debugging
            System.out.println("[Python] " + line);
            all.append(line).append(System.lineSeparator());
            if (line.trim().startsWith("{") || line.trim().startsWith("[")) {
                jsonLine = line.trim();
            }
        }
        if (jsonLine != null) {
            return jsonLine;
        }
        return all.toString().trim();
    }
}


