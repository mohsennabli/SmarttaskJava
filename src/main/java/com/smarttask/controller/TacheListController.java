package com.smarttask.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import com.smarttask.AppContext;
import com.smarttask.AppRouter;
import com.smarttask.model.Projet;
import com.smarttask.model.Tache;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class TacheListController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRANCE);

    private Integer projetFilter;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private GridPane gridContainer;
    @FXML
    private Label emptyLabel;
    @FXML
    private Label pageTitle;
    @FXML
    private Button dashboardButton;
    @FXML
    private VBox sidebarPanel;
    @FXML
    private HBox frontTopBar;

    @FXML
    private void initialize() {
        boolean admin = "BACKOFFICE".equalsIgnoreCase(AppContext.sessionService().getOffice());
        if (dashboardButton != null) {
            dashboardButton.setVisible(admin);
            dashboardButton.setManaged(admin);
        }
        if (sidebarPanel != null) {
            sidebarPanel.setVisible(admin);
            sidebarPanel.setManaged(admin);
        }
        if (frontTopBar != null) {
            frontTopBar.setVisible(!admin);
            frontTopBar.setManaged(!admin);
        }
        if (sortCombo != null) {
            sortCombo.setItems(javafx.collections.FXCollections.observableArrayList(
                    "Libelle A-Z",
                    "Libelle Z-A",
                    "Date limite proche",
                    "Date limite lointaine",
                    "Priorite haute",
                    "Priorite basse"));
            sortCombo.getSelectionModel().selectFirst();
        }
        refreshGrid();
    }

    public void setProjetFilter(Integer projetId) {
        this.projetFilter = projetId;
        if (pageTitle != null) {
            pageTitle.setText(projetId == null ? "Gestion des taches" : "Gestion des taches");
        }
        refreshGrid();
    }

    @FXML
    private void refreshGrid() {
        String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        List<Tache> filtered = AppContext.tacheRepository().findAll().stream()
                .filter(t -> projetFilter == null || t.getProjetId() == projetFilter)
                .filter(t -> search.isEmpty()
                        || t.getLibelle().toLowerCase(Locale.ROOT).contains(search)
                || findProjetName(t.getProjetId()).toLowerCase(Locale.ROOT).contains(search))
            .sorted(taskComparator())
                .collect(Collectors.toList());

        gridContainer.getChildren().clear();

        if (filtered.isEmpty()) {
            emptyLabel.setVisible(true);
            emptyLabel.setManaged(true);
            return;
        }

        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);

        int col = 0;
        int row = 0;
        for (Tache tache : filtered) {
            VBox card = buildCard(tache);
            gridContainer.add(card, col, row);
            col++;
            if (col == 2) {
                col = 0;
                row++;
            }
        }
    }

    @FXML
    private void clearSearch() {
        searchField.clear();
        refreshGrid();
    }

    @FXML
    private void sortChanged() {
        refreshGrid();
    }

    @FXML
    private void createTache() {
        AppRouter.showTacheForm(null, projetFilter);
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
        AppRouter.showTacheList(projetFilter);
    }

    @FXML
    private void logout() {
        AppContext.sessionService().logout();
        AppRouter.showLanding();
    }

    private VBox buildCard(Tache tache) {
        Label title = new Label(tache.getLibelle());
        title.getStyleClass().add("card-title");

        Label project = new Label("Projet: " + findProjetName(tache.getProjetId()));
        project.getStyleClass().add("meta-text");

        Label priority = new Label("Priorite: " + capitalize(tache.getPriorite()));
        priority.getStyleClass().add("chip");

        Label details = new Label("Etat: " + formatEtat(tache.getEtat()) + "   |   Date limite: "
                + tache.getDateLimite().format(DATE_FORMATTER));
        details.getStyleClass().add("meta-text");

        Button editBtn = new Button("Modifier");
        editBtn.getStyleClass().addAll("btn", "btn-outline");
        editBtn.setOnAction(event -> AppRouter.showTacheForm(tache));

        Button deleteBtn = new Button("Supprimer");
        deleteBtn.getStyleClass().addAll("btn", "btn-danger");
        deleteBtn.setOnAction(event -> {
            AppContext.tacheRepository().deleteById(tache.getId());
            refreshGrid();
        });

        HBox actions = new HBox(10, editBtn, deleteBtn);

        VBox card = new VBox(12, title, project, priority, details, actions);
        card.getStyleClass().add("entity-card");
        return card;
    }

    private String findProjetName(int projetId) {
        return AppContext.projetRepository().findById(projetId)
            .map(Projet::getNom)
            .orElse("Projet supprime");
    }

    private String formatEtat(String etat) {
        return switch (etat) {
            case "en_cours" -> "En cours";
            case "termine" -> "Termine";
            default -> "A faire";
        };
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    private Comparator<Tache> taskComparator() {
        String sort = sortCombo == null || sortCombo.getValue() == null ? "Libelle A-Z" : sortCombo.getValue();
        return switch (sort) {
            case "Libelle Z-A" -> Comparator.comparing(Tache::getLibelle, String.CASE_INSENSITIVE_ORDER.reversed());
            case "Date limite proche" -> Comparator.comparing(Tache::getDateLimite);
            case "Date limite lointaine" -> Comparator.comparing(Tache::getDateLimite).reversed();
            case "Priorite haute" -> Comparator.comparingInt((Tache t) -> priorityRank(t.getPriorite())).reversed();
            case "Priorite basse" -> Comparator.comparingInt((Tache t) -> priorityRank(t.getPriorite()));
            default -> Comparator.comparing(Tache::getLibelle, String.CASE_INSENSITIVE_ORDER);
        };
    }

    private int priorityRank(String priority) {
        if (priority == null) {
            return 0;
        }
        return switch (priority) {
            case "haute" -> 3;
            case "moyenne" -> 2;
            case "basse" -> 1;
            default -> 0;
        };
    }
}
