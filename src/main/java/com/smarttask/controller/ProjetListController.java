package com.smarttask.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import com.smarttask.AppContext;
import com.smarttask.AppRouter;
import com.smarttask.model.Projet;
import com.smarttask.dao.ProjetPdfExportService;
import com.smarttask.util.AlertUtil;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ProjetListController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRANCE);
    private final ProjetPdfExportService pdfExportService = new ProjetPdfExportService();

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private GridPane gridContainer;
    @FXML
    private Label emptyLabel;
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
                    "Nom A-Z",
                    "Nom Z-A",
                    "Date debut récente",
                    "Date debut ancienne",
                    "Echeance proche",
                    "Echeance lointaine"));
            sortCombo.getSelectionModel().selectFirst();
        }
        refreshGrid();
    }

    @FXML
    private void refreshGrid() {
        List<Projet> filtered = visibleProjects();

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
        for (Projet projet : filtered) {
            VBox card = buildCard(projet);
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
    private void exportPdf() {
        List<Projet> projets = visibleProjects();
        if (projets.isEmpty()) {
            AlertUtil.info("Export PDF", "Aucun projet ne correspond aux filtres actuels.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter les projets en PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        fileChooser.setInitialFileName("projets-" + LocalDate.now() + ".pdf");

        java.io.File selectedFile = fileChooser.showSaveDialog(searchField.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        try {
            Path output = selectedFile.toPath();
            if (!output.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                output = output.resolveSibling(output.getFileName() + ".pdf");
            }

            pdfExportService.exportProjects(projets, output);
            AlertUtil.info("Export PDF", "Le fichier a ete genere avec succes.\n" + output);
        } catch (Exception e) {
            AlertUtil.error("Export PDF", "Impossible de generer le PDF: " + e.getMessage());
        }
    }

    @FXML
    private void createProjet() {
        AppRouter.showProjetForm(null);
    }

    @FXML
    private void goDashboard() {
        AppRouter.showDashboard();
    }

    @FXML
    private void goFrontHome() {
        AppRouter.showFrontHome();
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

    private List<Projet> visibleProjects() {
        String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        return AppContext.projetRepository().findAll().stream()
                .filter(p -> search.isEmpty()
                        || p.getNom().toLowerCase(Locale.ROOT).contains(search)
                        || p.getDescription().toLowerCase(Locale.ROOT).contains(search))
                .sorted(projectComparator())
                .collect(Collectors.toList());
    }

    private VBox buildCard(Projet projet) {
        Label title = new Label(projet.getNom());
        title.getStyleClass().add("card-title");

        Label description = new Label(projet.getDescription());
        description.getStyleClass().add("card-text");
        description.setWrapText(true);

        Label status = new Label("Statut: " + formatStatut(projet.getStatut()));
        status.getStyleClass().add("chip");

        Label dates = new Label("Debut: " + projet.getDateDebut().format(DATE_FORMATTER) + "   |   Echeance: "
                + projet.getDateEcheance().format(DATE_FORMATTER));
        dates.getStyleClass().add("meta-text");

        Button editBtn = new Button("Modifier");
        editBtn.getStyleClass().addAll("btn", "btn-outline");
        editBtn.setOnAction(event -> AppRouter.showProjetForm(projet));

        Button tasksBtn = new Button("Voir taches");
        tasksBtn.getStyleClass().addAll("btn", "btn-primary");
        tasksBtn.setOnAction(event -> AppRouter.showTacheList(projet.getId()));

        Button deleteBtn = new Button("Supprimer");
        deleteBtn.getStyleClass().addAll("btn", "btn-danger");
        deleteBtn.setOnAction(event -> {
            try {
                AppContext.projectNotificationService().notifyDeleted(projet);
            } catch (Exception ignored) {
            }
            AppContext.projetRepository().deleteById(projet.getId());
            refreshGrid();
        });

        HBox actions = new HBox(10, tasksBtn, editBtn, deleteBtn);

        VBox card = new VBox(12, title, description, status, dates, actions);
        card.getStyleClass().add("entity-card");
        return card;
    }

    private String formatStatut(String statut) {
        return switch (statut) {
            case "actif" -> "Actif";
            case "termine" -> "Termine";
            default -> "En attente";
        };
    }

    private Comparator<Projet> projectComparator() {
        String sort = sortCombo == null || sortCombo.getValue() == null ? "Nom A-Z" : sortCombo.getValue();
        return switch (sort) {
            case "Nom Z-A" -> Comparator.comparing(Projet::getNom, String.CASE_INSENSITIVE_ORDER.reversed());
            case "Date debut récente" -> Comparator.comparing(Projet::getDateDebut).reversed();
            case "Date debut ancienne" -> Comparator.comparing(Projet::getDateDebut);
            case "Echeance proche" -> Comparator.comparing(Projet::getDateEcheance);
            case "Echeance lointaine" -> Comparator.comparing(Projet::getDateEcheance).reversed();
            default -> Comparator.comparing(Projet::getNom, String.CASE_INSENSITIVE_ORDER);
        };
    }
}
