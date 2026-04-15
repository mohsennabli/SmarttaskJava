package com.smarttask.controller;

import com.smarttask.dao.TicketDAO;
import com.smarttask.model.Ticket;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Date;
import java.util.Optional;

public class TicketController {

    @FXML
    private TextField titreField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private ComboBox<String> statutCombo;

    @FXML
    private ComboBox<String> prioriteCombo;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> filterStatutCombo;

    @FXML
    private ComboBox<String> filterPrioriteCombo;

    @FXML
    private TableView<Ticket> ticketTable;

    @FXML
    private TableColumn<Ticket, Integer> colId;

    @FXML
    private TableColumn<Ticket, String> colTitre;

    @FXML
    private TableColumn<Ticket, String> colStatut;

    @FXML
    private TableColumn<Ticket, String> colPriorite;

    @FXML
    private TableColumn<Ticket, Date> colDate;

    @FXML
    private Label lblStats;

    private TicketDAO dao;
    private ObservableList<Ticket> ticketList;

    @FXML
    public void initialize() {
        dao = new TicketDAO();
        ticketList = FXCollections.observableArrayList();

        setupTableColumns();
        setupComboBoxes();
        loadTickets();
        setupSelectionListener();
        updateStats();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colPriorite.setCellValueFactory(new PropertyValueFactory<>("priorite"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));

        // Formater la colonne date
        colDate.setCellFactory(column -> new TableCell<Ticket, Date>() {
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(item));
                }
            }
        });

        ticketTable.setItems(ticketList);
    }

    private void setupComboBoxes() {
        // Statuts possibles
        statutCombo.setItems(FXCollections.observableArrayList(
                "open", "in_progress", "resolved", "closed"
        ));
        statutCombo.setValue("open");

        // Priorités possibles
        prioriteCombo.setItems(FXCollections.observableArrayList(
                "low", "medium", "high", "urgent"
        ));
        prioriteCombo.setValue("medium");

        // Filtres
        filterStatutCombo.setItems(FXCollections.observableArrayList(
                "Tous", "open", "in_progress", "resolved", "closed"
        ));
        filterStatutCombo.setValue("Tous");

        filterPrioriteCombo.setItems(FXCollections.observableArrayList(
                "Tous", "low", "medium", "high", "urgent"
        ));
        filterPrioriteCombo.setValue("Tous");

        // Listeners pour les filtres
        filterStatutCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        filterPrioriteCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void setupSelectionListener() {
        ticketTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        displayTicketDetails(newSelection);
                    }
                }
        );
    }

    private void displayTicketDetails(Ticket ticket) {
        titreField.setText(ticket.getTitre());
        descriptionArea.setText(ticket.getDescription());
        statutCombo.setValue(ticket.getStatut());
        prioriteCombo.setValue(ticket.getPriorite());
    }

    private void loadTickets() {
        try {
            ticketList.setAll(dao.getAll());
        } catch (Exception e) {
            showAlert("Erreur", "Impossible de charger les tickets: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    private void applyFilters() {
        String statut = filterStatutCombo.getValue();
        String priorite = filterPrioriteCombo.getValue();

        if (("Tous".equals(statut) || statut == null) &&
                ("Tous".equals(priorite) || priorite == null)) {
            loadTickets();
        } else if (!"Tous".equals(statut) && "Tous".equals(priorite)) {
            try {
                ticketList.setAll(dao.filterByStatut(statut));
            } catch (Exception e) {
                showAlert("Erreur", "Erreur lors du filtrage: " + e.getMessage(),
                        Alert.AlertType.ERROR);
            }
        } else if ("Tous".equals(statut) && !"Tous".equals(priorite)) {
            try {
                ticketList.setAll(dao.filterByPriorite(priorite));
            } catch (Exception e) {
                showAlert("Erreur", "Erreur lors du filtrage: " + e.getMessage(),
                        Alert.AlertType.ERROR);
            }
        } else {
            try {
                ticketList.setAll(dao.filter(statut, priorite));
            } catch (Exception e) {
                showAlert("Erreur", "Erreur lors du filtrage: " + e.getMessage(),
                        Alert.AlertType.ERROR);
            }
        }

        updateStats();
    }

    @FXML
    public void addTicket() {
        // Validation
        if (!validateInput()) {
            return;
        }

        Ticket ticket = new Ticket();
        ticket.setTitre(titreField.getText().trim());
        ticket.setDescription(descriptionArea.getText().trim());
        ticket.setStatut(statutCombo.getValue());
        ticket.setPriorite(prioriteCombo.getValue());
        ticket.setDateCreation(new Date());

        try {
            dao.add(ticket);
            clearForm();
            applyFilters(); // Rafraîchir avec les filtres actuels
            updateStats();
            showAlert("Succès", "Ticket ajouté avec succès!", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ajouter le ticket: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void updateTicket() {
        Ticket selected = ticketTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Aucune sélection", "Veuillez sélectionner un ticket à modifier",
                    Alert.AlertType.WARNING);
            return;
        }

        if (!validateInput()) {
            return;
        }

        selected.setTitre(titreField.getText().trim());
        selected.setDescription(descriptionArea.getText().trim());
        selected.setStatut(statutCombo.getValue());
        selected.setPriorite(prioriteCombo.getValue());

        try {
            dao.update(selected);
            ticketTable.refresh();
            updateStats();
            showAlert("Succès", "Ticket modifié avec succès!", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Erreur", "Impossible de modifier le ticket: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void deleteTicket() {
        Ticket selected = ticketTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Aucune sélection", "Veuillez sélectionner un ticket à supprimer",
                    Alert.AlertType.WARNING);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous vraiment supprimer le ticket \"" + selected.getTitre() + "\" ?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                dao.delete(selected.getId());
                applyFilters(); // Rafraîchir avec les filtres actuels
                clearForm();
                updateStats();
                showAlert("Succès", "Ticket supprimé avec succès!", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Erreur", "Impossible de supprimer le ticket: " + e.getMessage(),
                        Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    public void searchTickets() {
        String keyword = searchField.getText().trim();

        if (keyword.isEmpty()) {
            applyFilters();
            return;
        }

        try {
            ticketList.setAll(dao.search(keyword));
            updateStats();
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de la recherche: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void clearFilters() {
        filterStatutCombo.setValue("Tous");
        filterPrioriteCombo.setValue("Tous");
        searchField.clear();
        loadTickets();
    }

    @FXML
    public void clearForm() {
        titreField.clear();
        descriptionArea.clear();
        statutCombo.setValue("open");
        prioriteCombo.setValue("medium");
        ticketTable.getSelectionModel().clearSelection();
    }

    @FXML
    public void refresh() {
        loadTickets();
        updateStats();
        showAlert("Info", "Liste des tickets rafraîchie", Alert.AlertType.INFORMATION);
    }

    private boolean validateInput() {
        if (titreField.getText() == null || titreField.getText().trim().isEmpty()) {
            showAlert("Validation", "Le titre est obligatoire", Alert.AlertType.WARNING);
            return false;
        }

        if (titreField.getText().length() > 255) {
            showAlert("Validation", "Le titre est trop long (max 255 caractères)",
                    Alert.AlertType.WARNING);
            return false;
        }

        return true;
    }

    private void updateStats() {
        try {
            var stats = dao.countByStatut();
            int total = ticketList.size();
            lblStats.setText(String.format("📊 Total: %d tickets | Open: %d | In Progress: %d | Resolved: %d | Closed: %d",
                    total,
                    stats.getOrDefault("open", 0),
                    stats.getOrDefault("in_progress", 0),
                    stats.getOrDefault("resolved", 0),
                    stats.getOrDefault("closed", 0)
            ));
        } catch (Exception e) {
            lblStats.setText("📊 Statistiques indisponibles");
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}