package com.smarttask.controller;

import com.smarttask.dao.TicketDAO;
import com.smarttask.model.Ticket;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.Date;

public class TicketController extends DashboardNavigationController {

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

    @FXML
    private Label totalTicketsLabel;

    @FXML
    private Label openTicketsLabel;

    @FXML
    private Label progressTicketsLabel;

    @FXML
    private Label resolvedTicketsLabel;

    private TicketDAO dao;
    private ObservableList<Ticket> ticketList;

    @FXML
    public void initialize() {
        initializeDashboardHeader();

        dao = new TicketDAO();
        ticketList = FXCollections.observableArrayList();

        setupTableColumns();
        setupComboBoxes();
        loadTickets();
        setupSelectionListener();
        updateStats();

        // Double-clic pour ouvrir les commentaires
        setupDoubleClickToOpenComments();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colPriorite.setCellValueFactory(new PropertyValueFactory<>("priorite"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));

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
        // ComboBox pour le formulaire
        statutCombo.setItems(FXCollections.observableArrayList(
                "open", "in_progress", "resolved", "closed"
        ));
        statutCombo.setValue("open");

        prioriteCombo.setItems(FXCollections.observableArrayList(
                "low", "medium", "high", "urgent"
        ));
        prioriteCombo.setValue("medium");

        // ComboBox pour les filtres (avec option "Tous")
        filterStatutCombo.setItems(FXCollections.observableArrayList(
                "Tous", "open", "in_progress", "resolved", "closed"
        ));
        filterStatutCombo.setValue("Tous");

        filterPrioriteCombo.setItems(FXCollections.observableArrayList(
                "Tous", "low", "medium", "high", "urgent"
        ));
        filterPrioriteCombo.setValue("Tous");

        // Listeners pour appliquer les filtres automatiquement
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

    private void setupDoubleClickToOpenComments() {
        ticketTable.setRowFactory(tv -> {
            TableRow<Ticket> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Ticket ticket = row.getItem();
                    openCommentaireInterface(ticket);
                }
            });
            return row;
        });
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

    // ========== MÉTHODES DE FILTRAGE ==========

    @FXML
    public void applyFilters() {
        String statut = filterStatutCombo.getValue();
        String priorite = filterPrioriteCombo.getValue();

        boolean filterStatut = statut != null && !"Tous".equals(statut);
        boolean filterPriorite = priorite != null && !"Tous".equals(priorite);

        try {
            if (filterStatut && filterPriorite) {
                ticketList.setAll(dao.filter(statut, priorite));
            } else if (filterStatut) {
                ticketList.setAll(dao.filterByStatut(statut));
            } else if (filterPriorite) {
                ticketList.setAll(dao.filterByPriorite(priorite));
            } else {
                ticketList.setAll(dao.getAll());
            }
            updateStats();
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors du filtrage: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void refresh() {
        applyFilters();
    }

    @FXML
    public void clearFilters() {
        filterStatutCombo.setValue("Tous");
        filterPrioriteCombo.setValue("Tous");
        searchField.clear();
        applyFilters();
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
            showAlert("Erreur", "Erreur lors de la recherche: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // ========== CRUD ==========

    @FXML
    public void addTicket() {
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
            applyFilters();
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
                applyFilters();
                clearForm();
                showAlert("Succès", "Ticket supprimé avec succès!", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Erreur", "Impossible de supprimer le ticket: " + e.getMessage(),
                        Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    public void clearForm() {
        titreField.clear();
        descriptionArea.clear();
        statutCombo.setValue("open");
        prioriteCombo.setValue("medium");
        ticketTable.getSelectionModel().clearSelection();
    }

    // ========== NAVIGATION ==========

    @FXML
    public void openCommentaires() {
        Ticket selected = ticketTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Aucune sélection", "Veuillez sélectionner un ticket pour voir ses commentaires.",
                    Alert.AlertType.WARNING);
            return;
        }

        openCommentaireInterface(selected);
    }

    private void openCommentaireInterface(Ticket ticket) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smarttask/Commentaire.fxml"));
            Parent root = loader.load();

            CommentaireController commentaireController = loader.getController();
            commentaireController.setTicketId(ticket.getId());
            commentaireController.setTicketTitle(ticket.getTitre());

            Stage stage = new Stage();
            stage.setTitle("Commentaires - Ticket #" + ticket.getId() + " : " + ticket.getTitre());
            stage.setScene(new Scene(root, 750, 550));
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.initOwner(ticketTable.getScene().getWindow());
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir les commentaires: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void openStatistiques() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smarttask/statistiques.fxml"));
            Parent root = loader.load();

            StatistiquesController statsController = loader.getController();
            statsController.setTickets(ticketList);

            Stage stage = new Stage();
            stage.setTitle("📊 Tableau de bord - SmartTask");
            stage.setScene(new Scene(root, 900, 750));
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.initOwner(ticketTable.getScene().getWindow());
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir les statistiques: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void openChatbot() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smarttask/chatbot.fxml"));
            Parent root = loader.load();

            ChatbotController chatbotController = loader.getController();
            chatbotController.setTickets(ticketList);

            Stage stage = new Stage();
            stage.setTitle("🤖 Assistant SmartTask");
            stage.setScene(new Scene(root, 480, 600));
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.initOwner(ticketTable.getScene().getWindow());
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir l'assistant: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void openCalendar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smarttask/calendar.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("📅 Calendrier des tickets - SmartTask");
            stage.setScene(new Scene(root, 1000, 700));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le calendrier: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    // ========== UTILITAIRES ==========

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

            if (totalTicketsLabel != null) totalTicketsLabel.setText(String.valueOf(total));
            if (openTicketsLabel != null) openTicketsLabel.setText(String.valueOf(stats.getOrDefault("open", 0)));
            if (progressTicketsLabel != null) progressTicketsLabel.setText(String.valueOf(stats.getOrDefault("in_progress", 0)));
            if (resolvedTicketsLabel != null) resolvedTicketsLabel.setText(String.valueOf(stats.getOrDefault("resolved", 0)));

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