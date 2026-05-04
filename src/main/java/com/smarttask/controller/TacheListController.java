package com.smarttask.controller;

import com.smarttask.dao.ProjetDAO;
import com.smarttask.dao.TacheDAO;
import com.smarttask.model.Projet;
import com.smarttask.model.Tache;
import com.smarttask.util.AppSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

public class TacheListController extends DashboardNavigationController implements Initializable {
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
    private Button addTacheBtn;

    private final TacheDAO tacheDAO = new TacheDAO();
    private final ProjetDAO projetDAO = new ProjetDAO();
    private Integer projetFilter = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeDashboardHeader();

        // Show addTacheBtn only if manager
        boolean isManager = "manager".equals(AppSession.getCurrentUser().getType());
        addTacheBtn.setVisible(isManager);
        addTacheBtn.setManaged(isManager);

        // Populate sortCombo
        sortCombo.getItems().addAll(
                "Libelle A-Z",
                "Libelle Z-A",
                "Date limite proche",
                "Date limite lointaine",
                "Priorite haute",
                "Priorite basse"
        );
        sortCombo.setValue("Libelle A-Z");

        // Listen to search and sort changes
        searchField.textProperty().addListener((obs, oldVal, newVal) -> refreshGrid());
        sortCombo.valueProperty().addListener((obs, oldVal, newVal) -> refreshGrid());

        // Load initial data
        refreshGrid();
    }

    public void setProjetFilter(Integer projetId) {
        this.projetFilter = projetId;
        if (projetId != null) {
            var projet = projetDAO.getProjetById(projetId);
            if (projet.isPresent()) {
                pageTitle.setText("Tâches du projet: " + projet.get().getNom());
            }
        }
        refreshGrid();
    }

    @FXML
    private void handleAddTache() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smarttask/tache-form.fxml"));
            Parent root = loader.load();
            TacheFormController controller = loader.getController();
            controller.setTacheToEdit(null);
            if (projetFilter != null) {
                controller.setSelectedProjetId(projetFilter);
            }

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle("Créer une tâche");
            modal.setScene(new Scene(root));
            modal.showAndWait();

            refreshGrid();
        } catch (IOException e) {
            System.err.println("Erreur lors de l'ouverture du formulaire: " + e.getMessage());
        }
    }

    @FXML
    private void handleBackToProjets() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smarttask/projet-list.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) gridContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur lors de la navigation: " + e.getMessage());
        }
    }

    @FXML
    protected void handleLogout() {
        try {
            AppSession.clear();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smarttask/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) gridContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur lors de la déconnexion: " + e.getMessage());
        }
    }

    private void refreshGrid() {
        gridContainer.getChildren().clear();
        
        List<Tache> taches;
        if (projetFilter != null) {
            taches = tacheDAO.getTachesByProjetId(projetFilter);
        } else {
            taches = tacheDAO.getAllTaches();
        }

        // Filter by search
        String searchText = searchField.getText().toLowerCase();
        List<Tache> filtered = taches.stream()
                .filter(t -> {
                    boolean matches = t.getLibelle().toLowerCase().contains(searchText);
                    if (!matches && projetFilter == null) {
                        var projet = projetDAO.getProjetById(t.getProjetId());
                        if (projet.isPresent()) {
                            matches = projet.get().getNom().toLowerCase().contains(searchText);
                        }
                    }
                    return matches;
                })
                .toList();

        // Sort
        List<Tache> sorted = new ArrayList<>(filtered);
        String sortBy = sortCombo.getValue();
        switch (sortBy) {
            case "Libelle Z-A" -> sorted.sort((a, b) -> b.getLibelle().compareTo(a.getLibelle()));
            case "Date limite proche" -> sorted.sort((a, b) -> a.getDateLimite().compareTo(b.getDateLimite()));
            case "Date limite lointaine" -> sorted.sort((a, b) -> b.getDateLimite().compareTo(a.getDateLimite()));
            case "Priorite haute" -> sorted.sort((a, b) -> comparePriorite(b.getPriorite(), a.getPriorite()));
            case "Priorite basse" -> sorted.sort((a, b) -> comparePriorite(a.getPriorite(), b.getPriorite()));
            default -> sorted.sort(Comparator.comparing(Tache::getLibelle));
        }

        // Show empty label if no taches
        if (sorted.isEmpty()) {
            emptyLabel.setVisible(true);
            emptyLabel.setManaged(true);
        } else {
            emptyLabel.setVisible(false);
            emptyLabel.setManaged(false);

            // Add tache cards to grid (2 columns)
            boolean isManager = "manager".equals(AppSession.getCurrentUser().getType());
            for (int i = 0; i < sorted.size(); i++) {
                int col = i % 2;
                int row = i / 2;
                gridContainer.add(createTacheCard(sorted.get(i), isManager), col, row);
            }
        }
    }

    private VBox createTacheCard(Tache tache, boolean isManager) {
        VBox card = new VBox();
        card.setStyle("-fx-border-color: #ddd; -fx-border-radius: 8; -fx-padding: 12;");
        card.setSpacing(8);

        Label titleLabel = new Label(tache.getLibelle());
        titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
        titleLabel.getStyleClass().add("card-title");

        var projet = projetDAO.getProjetById(tache.getProjetId());
        String projetName = projet.isPresent() ? projet.get().getNom() : "Projet inconnu";
        Label projetLabel = new Label("Projet: " + projetName);
        projetLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");

        Label prioriteLabel = new Label("Priorité: " + tache.getPriorite());
        prioriteLabel.setStyle("-fx-font-size: 11; -fx-padding: 4 8;");
        prioriteLabel.getStyleClass().add("chip");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Label statusLabel = new Label("État: " + tache.getEtat() + "  |  Date limite: " +
                tache.getDateLimite().format(formatter));
        statusLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #999;");
        statusLabel.getStyleClass().add("meta-text");

        HBox buttonBox = new HBox();
        buttonBox.setSpacing(8);
        buttonBox.setStyle("-fx-padding: 8 0 0 0;");

        if (isManager) {
            Button editBtn = new Button("Modifier");
            editBtn.setStyle("-fx-padding: 6 12;");
            editBtn.setOnAction(e -> editTache(tache));

            Button deleteBtn = new Button("Supprimer");
            deleteBtn.setStyle("-fx-padding: 6 12; -fx-text-fill: #d32f2f;");
            deleteBtn.setOnAction(e -> deleteTache(tache.getId()));

            buttonBox.getChildren().addAll(editBtn, deleteBtn);
        }

        card.getChildren().addAll(titleLabel, projetLabel, prioriteLabel, statusLabel, buttonBox);
        return card;
    }

    private void editTache(Tache tache) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smarttask/tache-form.fxml"));
            Parent root = loader.load();
            TacheFormController controller = loader.getController();
            controller.setTacheToEdit(tache);

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle("Modifier la tâche");
            modal.setScene(new Scene(root));
            modal.showAndWait();

            refreshGrid();
        } catch (IOException e) {
            System.err.println("Erreur lors de l'ouverture du formulaire: " + e.getMessage());
        }
    }

    private void deleteTache(int tacheId) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmer la suppression");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cette tâche ?");
        if (alert.showAndWait().get() == ButtonType.OK) {
            tacheDAO.deleteTache(tacheId);
            refreshGrid();
        }
    }

    private int comparePriorite(String p1, String p2) {
        int order1 = getPrioriteOrder(p1);
        int order2 = getPrioriteOrder(p2);
        return Integer.compare(order1, order2);
    }

    private int getPrioriteOrder(String priorite) {
        return switch (priorite) {
            case "basse" -> 0;
            case "moyenne" -> 1;
            case "haute" -> 2;
            default -> 0;
        };
    }
}

