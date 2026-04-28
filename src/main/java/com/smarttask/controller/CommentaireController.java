package com.smarttask.controller;

import com.smarttask.dao.CommentaireDAO;
import com.smarttask.model.Commentaire;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CommentaireController {

    @FXML
    private TextArea contenuField;

    @FXML
    private TableView<Commentaire> tableCommentaires;

    @FXML
    private TableColumn<Commentaire, String> colContenu;

    @FXML
    private TableColumn<Commentaire, Date> colDate;

    @FXML
    private Label ticketInfoLabel;

    @FXML
    private Label commentCountLabel;

    private CommentaireDAO commentaireDAO;
    private int currentTicketId;
    private String currentTicketTitle;

    @FXML
    public void initialize() {
        commentaireDAO = new CommentaireDAO();
        setupTableColumns();
    }

    private void setupTableColumns() {
        colContenu.setCellValueFactory(new PropertyValueFactory<>("contenu"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateCommentaire"));

        colDate.setCellFactory(column -> new TableCell<Commentaire, Date>() {
            private final SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(format.format(item));
                }
            }
        });
    }

    public void setTicketId(int ticketId) {
        this.currentTicketId = ticketId;
        loadComments();
        updateTicketInfo();
    }

    public void setTicketTitle(String ticketTitle) {
        this.currentTicketTitle = ticketTitle;
        updateTicketInfo();
    }

    private void updateTicketInfo() {
        if (ticketInfoLabel != null) {
            ticketInfoLabel.setText("Ticket #" + currentTicketId + " : " + currentTicketTitle);
        }
    }

    private void loadComments() {
        if (currentTicketId > 0) {
            try {
                var comments = commentaireDAO.getByTicket(currentTicketId);
                tableCommentaires.getItems().setAll(comments);
                updateCommentCount(comments.size());
            } catch (Exception e) {
                showAlert("Erreur", "Impossible de charger les commentaires: " + e.getMessage(),
                        Alert.AlertType.ERROR);
            }
        }
    }

    private void updateCommentCount(int count) {
        if (commentCountLabel != null) {
            String text = count + " commentaire";
            text += count > 1 ? "s" : "";
            commentCountLabel.setText(text);
        }
    }

    @FXML
    public void ajouterCommentaire() {
        if (currentTicketId == 0) {
            showAlert("Erreur", "Aucun ticket sélectionné", Alert.AlertType.ERROR);
            return;
        }

        String contenu = contenuField.getText();
        if (contenu == null || contenu.trim().isEmpty()) {
            showAlert("Erreur", "Le commentaire ne peut pas être vide", Alert.AlertType.WARNING);
            return;
        }

        if (contenu.length() > 1000) {
            showAlert("Erreur", "Le commentaire est trop long (max 1000 caractères)", Alert.AlertType.WARNING);
            return;
        }

        Commentaire commentaire = new Commentaire();
        commentaire.setContenu(contenu.trim());
        commentaire.setDateCommentaire(new Date());
        commentaire.setTicketId(currentTicketId);

        try {
            commentaireDAO.add(commentaire);
            loadComments();
            contenuField.clear();
            showAlert("Succès", "Commentaire ajouté avec succès!", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ajouter: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void supprimerCommentaire() {
        Commentaire selected = tableCommentaires.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Aucune sélection", "Veuillez sélectionner un commentaire à supprimer",
                    Alert.AlertType.WARNING);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous vraiment supprimer ce commentaire ?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                commentaireDAO.delete(selected.getId());
                loadComments();
                showAlert("Succès", "Commentaire supprimé avec succès!", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Erreur", "Impossible de supprimer: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    public void refresh() {
        loadComments();
        showAlert("Info", "Commentaires rafraîchis", Alert.AlertType.INFORMATION);
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