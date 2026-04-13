package org.esprit.gestionprojet.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.esprit.gestionprojet.AppContext;
import org.esprit.gestionprojet.AppRouter;
import org.esprit.gestionprojet.model.Projet;
import org.esprit.gestionprojet.util.AlertUtil;

import java.time.LocalDate;

public class ProjetFormController {
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
    private Button dashboardButton;
    @FXML
    private VBox sidebarPanel;
    @FXML
    private HBox frontTopBar;
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

    private Projet projetToEdit;

    @FXML
    private void initialize() {
        statutCombo.setItems(FXCollections.observableArrayList("en_attente", "actif", "termine"));
        statutCombo.getSelectionModel().select("en_attente");
        updateDashboardButtonVisibility();

        boolean admin = "BACKOFFICE".equalsIgnoreCase(AppContext.sessionService().getOffice());
        if (sidebarPanel != null) {
            sidebarPanel.setVisible(admin);
            sidebarPanel.setManaged(admin);
        }
        if (frontTopBar != null) {
            frontTopBar.setVisible(!admin);
            frontTopBar.setManaged(!admin);
        }
    }

    public void setProjetToEdit(Projet projet) {
        this.projetToEdit = projet;
        if (projet == null) {
            pageTitle.setText("Creer un projet");
            return;
        }

        pageTitle.setText("Modifier le projet");
        nomField.setText(projet.getNom());
        descriptionArea.setText(projet.getDescription());
        dateDebutPicker.setValue(projet.getDateDebut());
        dateEcheancePicker.setValue(projet.getDateEcheance());
        statutCombo.getSelectionModel().select(projet.getStatut());
    }

    @FXML
    private void saveProjet() {
        clearErrors();

        String nom = safeValue(nomField.getText());
        String description = safeValue(descriptionArea.getText());
        LocalDate dateDebut = dateDebutPicker.getValue();
        LocalDate dateEcheance = dateEcheancePicker.getValue();
        String statut = statutCombo.getValue();

        boolean valid = true;

        if (nom.isEmpty()) {
            nomError.setText("Le nom est obligatoire.");
            valid = false;
        }
        if (description.isEmpty()) {
            descriptionError.setText("La description est obligatoire.");
            valid = false;
        } else if (description.length() <= 10) {
            descriptionError.setText("La description doit contenir plus de 10 caracteres.");
            valid = false;
        }
        if (dateDebut == null) {
            dateDebutError.setText("La date de debut est obligatoire.");
            valid = false;
        } else if (!dateDebut.isAfter(LocalDate.now())) {
            dateDebutError.setText("La date de debut doit etre superieure a aujourd'hui.");
            valid = false;
        }
        if (dateEcheance == null) {
            dateEcheanceError.setText("La date d'echeance est obligatoire.");
            valid = false;
        }
        if (statut == null || statut.isEmpty()) {
            statutError.setText("Le statut est obligatoire.");
            valid = false;
        }

        if (!valid) {
            return;
        }

        if (!dateEcheance.isAfter(dateDebut)) {
            dateEcheanceError.setText("La date d'echeance doit etre superieure a la date de debut.");
            return;
        }

        if (projetToEdit == null) {
            Projet created = new Projet(0, nom, description, dateDebut, dateEcheance, statut);
            int createdId = AppContext.projetRepository().insert(created);
            created.setId(createdId);
            AlertUtil.info("Projet", "Projet cree avec succes.");
        } else {
            projetToEdit.setNom(nom);
            projetToEdit.setDescription(description);
            projetToEdit.setDateDebut(dateDebut);
            projetToEdit.setDateEcheance(dateEcheance);
            projetToEdit.setStatut(statut);
            AppContext.projetRepository().update(projetToEdit);
            AlertUtil.info("Projet", "Projet modifie avec succes.");
        }

        AppRouter.showProjetList();
    }

    @FXML
    private void cancel() {
        AppRouter.showProjetList();
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

    private String safeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private void clearErrors() {
        nomError.setText("");
        descriptionError.setText("");
        dateDebutError.setText("");
        dateEcheanceError.setText("");
        statutError.setText("");
    }

    private void updateDashboardButtonVisibility() {
        if (dashboardButton == null) {
            return;
        }

        boolean admin = "BACKOFFICE".equalsIgnoreCase(AppContext.sessionService().getOffice());
        dashboardButton.setVisible(admin);
        dashboardButton.setManaged(admin);
    }
}
