package com.smarttask.controller;

import com.smarttask.dao.ProjetDAO;
import com.smarttask.model.Projet;
import com.smarttask.util.AppSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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

public class ProjetListController implements Initializable {
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private GridPane gridContainer;
    @FXML
    private Label emptyLabel;
    @FXML
    private Button addProjetBtn;

    private final ProjetDAO projetDAO = new ProjetDAO();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Show addProjetBtn only if manager
        boolean isManager = "manager".equals(AppSession.getCurrentUser().getType());
        addProjetBtn.setVisible(isManager);
        addProjetBtn.setManaged(isManager);

        // Populate sortCombo
        sortCombo.getItems().addAll(
                "Nom A-Z",
                "Nom Z-A",
                "Date debut récente",
                "Date debut ancienne",
                "Echeance proche",
                "Echeance lointaine"
        );
        sortCombo.setValue("Nom A-Z");

        // Listen to search and sort changes
        searchField.textProperty().addListener((obs, oldVal, newVal) -> refreshGrid());
        sortCombo.valueProperty().addListener((obs, oldVal, newVal) -> refreshGrid());

        // Load initial data
        refreshGrid();
    }

    @FXML
    private void handleAddProjet() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smarttask/projet-form.fxml"));
            Parent root = loader.load();
            ProjetFormController controller = loader.getController();
            controller.setProjetToEdit(null);

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle("Créer un projet");
            modal.setScene(new Scene(root));
            modal.showAndWait();

            refreshGrid();
        } catch (IOException e) {
            System.err.println("Erreur lors de l'ouverture du formulaire: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
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
        List<Projet> projects = projetDAO.getAllProjets();

        // Filter by search
        String searchText = searchField.getText().toLowerCase();
        List<Projet> filtered = projects.stream()
                .filter(p -> p.getNom().toLowerCase().contains(searchText) ||
                        p.getDescription().toLowerCase().contains(searchText))
                .toList();

        // Sort
        List<Projet> sorted = new ArrayList<>(filtered);
        String sortBy = sortCombo.getValue();
        switch (sortBy) {
            case "Nom Z-A" -> sorted.sort((a, b) -> b.getNom().compareTo(a.getNom()));
            case "Date debut récente" -> sorted.sort((a, b) -> b.getDateDebut().compareTo(a.getDateDebut()));
            case "Date debut ancienne" -> sorted.sort((a, b) -> a.getDateDebut().compareTo(b.getDateDebut()));
            case "Echeance proche" -> sorted.sort((a, b) -> a.getDateEcheance().compareTo(b.getDateEcheance()));
            case "Echeance lointaine" -> sorted.sort((a, b) -> b.getDateEcheance().compareTo(a.getDateEcheance()));
            default -> sorted.sort(Comparator.comparing(Projet::getNom));
        }

        // Show empty label if no projects
        if (sorted.isEmpty()) {
            emptyLabel.setVisible(true);
            emptyLabel.setManaged(true);
        } else {
            emptyLabel.setVisible(false);
            emptyLabel.setManaged(false);

            // Add project cards to grid (2 columns)
            boolean isManager = "manager".equals(AppSession.getCurrentUser().getType());
            for (int i = 0; i < sorted.size(); i++) {
                int col = i % 2;
                int row = i / 2;
                gridContainer.add(createProjectCard(sorted.get(i), isManager), col, row);
            }
        }
    }

    private VBox createProjectCard(Projet projet, boolean isManager) {
        VBox card = new VBox();
        card.setStyle("-fx-border-color: #ddd; -fx-border-radius: 8; -fx-padding: 12;");
        card.setSpacing(8);

        Label titleLabel = new Label(projet.getNom());
        titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
        titleLabel.getStyleClass().add("card-title");

        Label descLabel = new Label(projet.getDescription());
        descLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #666; -fx-wrap-text: true;");
        descLabel.getStyleClass().add("card-text");
        descLabel.setWrapText(true);

        Label statutLabel = new Label("Statut: " + projet.getStatut());
        statutLabel.setStyle("-fx-font-size: 11; -fx-padding: 4 8;");
        statutLabel.getStyleClass().add("chip");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Label datesLabel = new Label("Début: " + projet.getDateDebut().format(formatter) +
                "  |  Échéance: " + projet.getDateEcheance().format(formatter));
        datesLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #999;");
        datesLabel.getStyleClass().add("meta-text");

        HBox buttonBox = new HBox();
        buttonBox.setSpacing(8);
        buttonBox.setStyle("-fx-padding: 8 0 0 0;");

        Button voirBtn = new Button("Voir tâches");
        voirBtn.setStyle("-fx-padding: 6 12;");
        voirBtn.setOnAction(e -> vTachesForProject(projet.getId()));

        buttonBox.getChildren().add(voirBtn);

        if (isManager) {
            Button editBtn = new Button("Modifier");
            editBtn.setStyle("-fx-padding: 6 12;");
            editBtn.setOnAction(e -> editProjet(projet));

            Button deleteBtn = new Button("Supprimer");
            deleteBtn.setStyle("-fx-padding: 6 12; -fx-text-fill: #d32f2f;");
            deleteBtn.setOnAction(e -> deleteProjet(projet.getId()));

            buttonBox.getChildren().addAll(editBtn, deleteBtn);
        }

        card.getChildren().addAll(titleLabel, descLabel, statutLabel, datesLabel, buttonBox);
        return card;
    }

    private void vTachesForProject(int projetId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smarttask/tache-list.fxml"));
            Parent root = loader.load();
            TacheListController controller = loader.getController();
            controller.setProjetFilter(projetId);

            Stage stage = (Stage) gridContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur lors de la navigation: " + e.getMessage());
        }
    }

    private void editProjet(Projet projet) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smarttask/projet-form.fxml"));
            Parent root = loader.load();
            ProjetFormController controller = loader.getController();
            controller.setProjetToEdit(projet);

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle("Modifier le projet");
            modal.setScene(new Scene(root));
            modal.showAndWait();

            refreshGrid();
        } catch (IOException e) {
            System.err.println("Erreur lors de l'ouverture du formulaire: " + e.getMessage());
        }
    }

    private void deleteProjet(int projetId) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmer la suppression");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer ce projet ?");
        if (alert.showAndWait().get() == ButtonType.OK) {
            projetDAO.deleteProjet(projetId);
            refreshGrid();
        }
    }
}

