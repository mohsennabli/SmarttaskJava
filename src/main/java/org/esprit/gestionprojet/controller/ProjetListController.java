package org.esprit.gestionprojet.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.esprit.gestionprojet.AppContext;
import org.esprit.gestionprojet.AppRouter;
import org.esprit.gestionprojet.model.Projet;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ProjetListController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRANCE);

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
        String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        List<Projet> filtered = AppContext.projetRepository().findAll().stream()
                .filter(p -> search.isEmpty()
                        || p.getNom().toLowerCase(Locale.ROOT).contains(search)
                        || p.getDescription().toLowerCase(Locale.ROOT).contains(search))
            .sorted(projectComparator())
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
