package com.smarttask.model;

import java.util.Date;

public class Ticket {

        private int id;
        private String titre;
        private String description;
        private String statut;
        private String priorite;
        private Date dateCreation;

        public Ticket() {}

        public Ticket(String titre, String description, String statut, String priorite, Date dateCreation) {
            this.titre = titre;
            this.description = description;
            this.statut = statut;
            this.priorite = priorite;
            this.dateCreation = dateCreation;
        }

    public int getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public String getDescription() {
        return description;
    }

    public String getStatut() {
        return statut;
    }

    public String getPriorite() {
        return priorite;
    }

    public Date getDateCreation() {
        return dateCreation;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public void setPriorite(String priorite) {
        this.priorite = priorite;
    }

    public void setDateCreation(Date dateCreation) {
        this.dateCreation = dateCreation;
    }
}

