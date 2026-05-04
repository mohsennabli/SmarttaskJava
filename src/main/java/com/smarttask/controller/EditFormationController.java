package com.smarttask.controller;

import com.smarttask.model.Formation;
import com.smarttask.service.FormationCrudResult;
import com.smarttask.service.FormationService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class EditFormationController implements Initializable {

    @FXML
    private TextField titreField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private DatePicker dateDebutPicker;

    @FXML
    private DatePicker dateFinPicker;

    @FXML
    private TextField dureeField;

    @FXML
    private TextField niveauField;

    @FXML
    private TextField categorieField;

    @FXML
    private TextField statutField;

    @FXML
    private TextField capacityField;

    @FXML
    private TextField googleEventIdField;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    private Integer editingFormationId;
    private final FormationService formationService = new FormationService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Defaults applied in prepareCreate.
    }

    public void prepareCreate() {
        editingFormationId = null;
        titreField.clear();
        descriptionArea.clear();
        dateDebutPicker.setValue(LocalDate.now());
        dateFinPicker.setValue(LocalDate.now());
        dureeField.clear();
        niveauField.clear();
        categorieField.clear();
        statutField.setText("active");
        capacityField.clear();
        googleEventIdField.clear();
    }

    public void prepareEdit(Formation f) {
        if (f == null) {
            prepareCreate();
            return;
        }
        editingFormationId = f.getId();
        titreField.setText(f.getTitre() != null ? f.getTitre() : "");
        descriptionArea.setText(f.getDescription() != null ? f.getDescription() : "");
        dateDebutPicker.setValue(f.getDateDebut());
        dateFinPicker.setValue(f.getDateFin());
        dureeField.setText(f.getDuree() != null ? String.valueOf(f.getDuree()) : "");
        niveauField.setText(f.getNiveau() != null ? f.getNiveau() : "");
        categorieField.setText(f.getCategorie() != null ? f.getCategorie() : "");
        statutField.setText(f.getStatut() != null ? f.getStatut() : "active");
        capacityField.setText(f.getCapacity() != null ? String.valueOf(f.getCapacity()) : "");
        googleEventIdField.setText(f.getGoogleEventId() != null ? f.getGoogleEventId() : "");
    }

    @FXML
    private void handleSave(ActionEvent event) {
        if (!isOptionalWholeNumber(dureeField.getText()) || !isOptionalWholeNumber(capacityField.getText())) {
            showAlert(Alert.AlertType.WARNING, "Validation",
                    "Duration and capacity must be whole numbers, or left empty.");
            return;
        }
        Formation formation = readFormationFromForm();
        if (formation == null) {
            showAlert(Alert.AlertType.WARNING, "Validation",
                    "Title, start and end dates, level, and status are required.");
            return;
        }

        FormationCrudResult result;
        if (editingFormationId == null) {
            result = formationService.createFormation(formation);
        } else {
            formation.setId(editingFormationId);
            result = formationService.updateFormation(formation);
        }

        switch (result) {
            case SUCCESS -> {
                showAlert(Alert.AlertType.INFORMATION, "Saved", "Formation saved successfully.");
                closeStage();
            }
            case FORBIDDEN -> showAlert(Alert.AlertType.ERROR, "Forbidden", "Only managers can change formations.");
            case VALIDATION_ERROR -> showAlert(Alert.AlertType.WARNING, "Validation",
                    "Please check required fields (title, dates, level, status).");
            case NOT_FOUND -> showAlert(Alert.AlertType.ERROR, "Not found", "Formation no longer exists.");
            case DB_ERROR -> showAlert(Alert.AlertType.ERROR, "Error", "Could not save to the database.");
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeStage();
    }

    private Formation readFormationFromForm() {
        String titre = trimOrNull(titreField.getText());
        if (titre == null) {
            return null;
        }
        LocalDate dd = dateDebutPicker.getValue();
        LocalDate df = dateFinPicker.getValue();
        if (dd == null || df == null) {
            return null;
        }
        String niveau = trimOrNull(niveauField.getText());
        String statut = trimOrNull(statutField.getText());
        if (niveau == null || statut == null) {
            return null;
        }

        Formation f = new Formation();
        f.setTitre(titre);
        f.setDescription(trimOrEmptyToNull(descriptionArea.getText()));
        f.setDateDebut(dd);
        f.setDateFin(df);
        f.setDuree(parseOptionalPositiveInt(dureeField.getText()));
        f.setNiveau(niveau);
        f.setCategorie(trimOrEmptyToNull(categorieField.getText()));
        f.setStatut(statut);
        f.setCapacity(parseOptionalPositiveInt(capacityField.getText()));
        f.setGoogleEventId(trimOrEmptyToNull(googleEventIdField.getText()));
        return f;
    }

    private static String trimOrNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String trimOrEmptyToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static boolean isOptionalWholeNumber(String raw) {
        if (raw == null || raw.isBlank()) {
            return true;
        }
        try {
            Integer.parseInt(raw.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static Integer parseOptionalPositiveInt(String s) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty()) {
            return null;
        }
        try {
            int v = Integer.parseInt(t);
            return v >= 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void closeStage() {
        ((Stage) cancelButton.getScene().getWindow()).close();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
