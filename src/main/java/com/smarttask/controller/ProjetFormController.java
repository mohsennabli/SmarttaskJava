package com.smarttask.controller;

import com.smarttask.dao.ProjetDAO;
import com.smarttask.model.Projet;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class ProjetFormController implements Initializable {
    @FXML
    private Label pageTitle;
    @FXML
    private TextField nomField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private DatePicker dateDebutPicker;
    @FXML
    private DatePicker dateEcheancePicker;
    @FXML
    private ComboBox<String> statutCombo;

    @FXML
    private Label nomError;
    @FXML
    private Label descriptionError;
    @FXML
    private Label dateDebutError;
    @FXML
    private Label dateEcheanceError;
    @FXML
    private Label statutError;

    @FXML
    private Button saveBtn;
    @FXML
    private Button cancelBtn;

    private final ProjetDAO projetDAO = new ProjetDAO();
    private Projet projetToEdit;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        statutCombo.getItems().addAll("en_attente", "actif", "termine");
        statutCombo.setValue("en_attente");
    }

    public void setProjetToEdit(Projet projet) {
        this.projetToEdit = projet;

        if (projet == null) {
            pageTitle.setText("Créer un projet");
        } else {
            pageTitle.setText("Modifier le projet");
            nomField.setText(projet.getNom());
            descriptionArea.setText(projet.getDescription());
            dateDebutPicker.setValue(projet.getDateDebut());
            dateEcheancePicker.setValue(projet.getDateEcheance());
            statutCombo.setValue(projet.getStatut());
        }
    }

    @FXML
    private void saveProjet() {
        // Clear errors
        nomError.setText("");
        descriptionError.setText("");
        dateDebutError.setText("");
        dateEcheanceError.setText("");
        statutError.setText("");

        // Validate
        String nom = nomField.getText().trim();
        String description = descriptionArea.getText().trim();
        LocalDate dateDebut = dateDebutPicker.getValue();
        LocalDate dateEcheance = dateEcheancePicker.getValue();
        String statut = statutCombo.getValue();

        boolean isValid = true;

        if (nom.isEmpty()) {
            nomError.setText("Le nom est obligatoire");
            isValid = false;
        }

        if (description.isEmpty() || description.length() < 10) {
            descriptionError.setText("La description doit avoir au minimum 10 caractères");
            isValid = false;
        }

        if (dateDebut == null || !dateDebut.isAfter(LocalDate.now())) {
            dateDebutError.setText("La date de début doit être après aujourd'hui");
            isValid = false;
        }

        if (dateEcheance == null || dateDebut != null && !dateEcheance.isAfter(dateDebut)) {
            dateEcheanceError.setText("La date d'échéance doit être après la date de début");
            isValid = false;
        }

        if (statut == null) {
            statutError.setText("Le statut est obligatoire");
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        Projet projet = new Projet(nom, description, dateDebut, dateEcheance, statut);

        boolean success;
        if (projetToEdit == null) {
            // Create new
            int id = projetDAO.insertProjet(projet);
            success = id > 0;
        } else {
            // Edit existing
            projet.setId(projetToEdit.getId());
            success = projetDAO.updateProjet(projet);
        }

        if (success) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setContentText("Le projet a été enregistré avec succès");
            alert.showAndWait();
            closeWindow();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setContentText("Erreur lors de l'enregistrement du projet");
            alert.showAndWait();
        }
    }

    @FXML
    private void cancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.close();
    }
}
