package com.smarttask.model;

import java.util.Date;

public class Commentaire {
    private int id;
    private String contenu;
    private Date dateCommentaire;
    private int ticketId;

    public Commentaire() {}

    public Commentaire(String contenu, Date dateCommentaire, int ticketId) {
        this.contenu = contenu;
        this.dateCommentaire = dateCommentaire;
        this.ticketId = ticketId;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public Date getDateCommentaire() {
        return dateCommentaire;
    }

    public void setDateCommentaire(Date dateCommentaire) {
        this.dateCommentaire = dateCommentaire;
    }

    public int getTicketId() {
        return ticketId;
    }

    public void setTicketId(int ticketId) {
        this.ticketId = ticketId;
    }

    @Override
    public String toString() {
        return "Commentaire{" +
                "id=" + id +
                ", contenu='" + contenu + '\'' +
                ", dateCommentaire=" + dateCommentaire +
                ", ticketId=" + ticketId +
                '}';
    }
}