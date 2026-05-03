package com.smarttask.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import com.smarttask.AppContext;
import com.smarttask.AppRouter;
import com.smarttask.model.Projet;
import com.smarttask.model.Tache;
import com.smarttask.dao.ProjectTaskSuggestion;
import com.smarttask.util.AlertUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
    @FXML
    private VBox draftPanel;
    @FXML
    private Label draftInfoLabel;
    @FXML
    private VBox draftSuggestionsBox;

    private Projet projetToEdit;
    private final List<ProjectTaskSuggestion> draftSuggestions = new ArrayList<>();

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
        hideDraftPanel();
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
            projetToEdit = new Projet(0, nom, description, dateDebut, dateEcheance, statut);
            int createdId = AppContext.projetRepository().insert(projetToEdit);
            projetToEdit.setId(createdId);
            notifyProjectCreated();
            pageTitle.setText("Brouillon du projet");
            AlertUtil.info("Projet", "Projet en brouillon enregistre. Vous pouvez maintenant valider les taches suggerees.");
        } else {
            projetToEdit.setNom(nom);
            projetToEdit.setDescription(description);
            projetToEdit.setDateDebut(dateDebut);
            projetToEdit.setDateEcheance(dateEcheance);
            projetToEdit.setStatut(statut);
            AppContext.projetRepository().update(projetToEdit);
            notifyProjectUpdated();
            AlertUtil.info("Projet", "Projet mis a jour. Les suggestions de taches ont ete rafraichies.");
        }

        loadDraftSuggestions();
    }

    @FXML
    private void refreshSuggestions() {
        if (projetToEdit == null || projetToEdit.getId() <= 0) {
            AlertUtil.info("Brouillon", "Enregistrez d'abord le projet pour generer des taches.");
            return;
        }

        loadDraftSuggestions();
    }

    private void loadDraftSuggestions() {
        if (projetToEdit == null || projetToEdit.getId() <= 0) {
            hideDraftPanel();
            return;
        }

        draftSuggestions.clear();
        draftSuggestions.addAll(AppContext.groqTaskRecommendationService().recommendTasks(projetToEdit));
        renderDraftSuggestions();
    }

    private void renderDraftSuggestions() {
        if (draftPanel == null || draftSuggestionsBox == null) {
            return;
        }

        draftPanel.setVisible(true);
        draftPanel.setManaged(true);
        draftSuggestionsBox.getChildren().clear();

        if (draftInfoLabel != null) {
            draftInfoLabel.setText(draftSuggestions.isEmpty()
                    ? "Aucune suggestion disponible."
                    : "Validez les taches que vous souhaitez insérer dans la base de donnees.");
        }

        for (ProjectTaskSuggestion suggestion : draftSuggestions) {
            draftSuggestionsBox.getChildren().add(buildSuggestionCard(suggestion));
        }
    }

    private VBox buildSuggestionCard(ProjectTaskSuggestion suggestion) {
        Label title = new Label(suggestion.libelle());
        title.getStyleClass().add("card-title");

        Label meta = new Label("Priorite: " + capitalize(suggestion.priorite())
                + "   |   Delai: " + suggestion.delayDays() + " jours");
        meta.getStyleClass().add("meta-text");

        Label reason = new Label(suggestion.reason());
        reason.setWrapText(true);
        reason.getStyleClass().add("card-text");

        Button approveBtn = new Button("Approuver");
        approveBtn.getStyleClass().addAll("btn", "btn-primary");
        approveBtn.setOnAction(event -> approveSuggestion(suggestion));

        Button cancelBtn = new Button("Ignorer");
        cancelBtn.getStyleClass().addAll("btn", "btn-outline");
        cancelBtn.setOnAction(event -> cancelSuggestion(suggestion));

        HBox actions = new HBox(10, approveBtn, cancelBtn);

        VBox card = new VBox(10, title, meta, reason, actions);
        card.getStyleClass().add("entity-card");
        return card;
    }

    private void approveSuggestion(ProjectTaskSuggestion suggestion) {
        if (projetToEdit == null || projetToEdit.getId() <= 0) {
            AlertUtil.error("Tache", "Le projet doit etre enregistre avant d'approuver une tache.");
            return;
        }

        LocalDate baseDate = projetToEdit.getDateDebut() == null ? LocalDate.now() : projetToEdit.getDateDebut();
        LocalDate dueDate = baseDate.plusDays(Math.max(1, suggestion.delayDays()));
        if (!dueDate.isAfter(LocalDate.now())) {
            dueDate = LocalDate.now().plusDays(Math.max(1, suggestion.delayDays()));
        }

        Tache tache = new Tache(0, suggestion.libelle(), suggestion.priorite(), dueDate, "a_faire", projetToEdit.getId());
        AppContext.tacheRepository().insert(tache);
        draftSuggestions.remove(suggestion);
        renderDraftSuggestions();
        AlertUtil.info("Tache", "Tache ajoutee au projet avec succes.");
    }

    private void cancelSuggestion(ProjectTaskSuggestion suggestion) {
        draftSuggestions.remove(suggestion);
        renderDraftSuggestions();
    }

    private void notifyProjectCreated() {
        try {
            AppContext.projectNotificationService().notifyCreated(projetToEdit, List.of());
        } catch (Exception ignored) {
        }
    }

    private void notifyProjectUpdated() {
        try {
            AppContext.projectNotificationService().notifyUpdated(projetToEdit);
        } catch (Exception ignored) {
        }
    }

    private void hideDraftPanel() {
        if (draftPanel != null) {
            draftPanel.setVisible(false);
            draftPanel.setManaged(false);
        }
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

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1);
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
