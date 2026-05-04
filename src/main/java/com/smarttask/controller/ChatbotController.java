package com.smarttask.controller;

import com.smarttask.model.ChatMessage;
import com.smarttask.model.Ticket;
import com.smarttask.service.ChatbotService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;

import java.util.List;

public class ChatbotController extends DashboardNavigationController {

    @FXML private ListView<ChatMessage> chatListView;
    @FXML private TextField inputField;
    @FXML private Label statusLabel;

    private ObservableList<ChatMessage> messages;
    private ChatbotService chatbotService;
    private List<Ticket> tickets;

    @FXML
    public void initialize() {
        initializeDashboardHeader();

        messages = FXCollections.observableArrayList();
        chatListView.setItems(messages);

        // Style personnalisé des messages
        chatListView.setCellFactory(lv -> new ListCell<ChatMessage>() {
            @Override
            protected void updateItem(ChatMessage item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Style différent pour l'utilisateur et l'assistant
                    String style;
                    if (item.getExpediteur().equals("Moi")) {
                        style = "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-padding: 10 15; -fx-background-radius: 15 15 0 15;";
                    } else {
                        style = "-fx-background-color: #f1f5f9; -fx-text-fill: #1e293b; -fx-padding: 10 15; -fx-background-radius: 15 15 15 0;";
                    }

                    Label messageLabel = new Label(item.getExpediteur() + " (" + item.getHeure() + "):\n" + item.getContenu());
                    messageLabel.setStyle(style);
                    messageLabel.setWrapText(true);
                    messageLabel.setMaxWidth(400);
                    setGraphic(messageLabel);
                }
            }
        });

        // Envoyer avec la touche Entrée
        inputField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                sendMessage();
            }
        });

        chatbotService = new ChatbotService();

        // Message de bienvenue
        addBotMessage("👋 Bonjour ! Je suis votre assistant SmartTask.\n\nTapez **'aide'** pour voir toutes mes commandes !");
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
        chatbotService.setTickets(tickets);
        statusLabel.setText("🤖 Chatbot prêt - " + tickets.size() + " tickets");
    }

    @FXML
    public void sendMessage() {
        String message = inputField.getText().trim();
        if (message.isEmpty()) return;

        addUserMessage(message);
        inputField.clear();

        String reponse = chatbotService.processMessage(message);

        // Petit délai pour simuler la réflexion de l'IA
        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
        delay.setOnFinished(event -> addBotMessage(reponse));
        delay.play();
    }

    @FXML
    public void sendSuggestion(ActionEvent event) {
        Button btn = (Button) event.getSource();
        String suggestion = btn.getText();
        inputField.setText(suggestion);
        sendMessage();
    }

    private void addUserMessage(String message) {
        messages.add(new ChatMessage(message, "Moi"));
        scrollToBottom();
    }

    private void addBotMessage(String message) {
        messages.add(new ChatMessage(message, "🤖 Assistant"));
        scrollToBottom();
    }

    private void scrollToBottom() {
        chatListView.scrollTo(messages.size() - 1);
    }

    @FXML
    public void clearChat() {
        messages.clear();
        addBotMessage("🧹 Chat nettoyé !\n\nComment puis-je vous aider ?");
    }
}