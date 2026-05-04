package com.smarttask.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatMessage {

    private String contenu;
    private String expediteur;
    private String heure;
    private LocalDateTime timestamp;

    public ChatMessage(String contenu, String expediteur) {
        this.contenu = contenu;
        this.expediteur = expediteur;
        this.timestamp = LocalDateTime.now();
        this.heure = timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public String getContenu() { return contenu; }
    public String getExpediteur() { return expediteur; }
    public String getHeure() { return heure; }
    public LocalDateTime getTimestamp() { return timestamp; }
}