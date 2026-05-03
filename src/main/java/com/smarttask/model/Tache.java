package com.smarttask.model;

import java.time.LocalDate;

public class Tache {
    private int id;
    private String libelle;
    private String priorite;
    private LocalDate dateLimite;
    private String etat;
    private int projetId;

    public Tache(int id, String libelle, String priorite, LocalDate dateLimite, String etat, int projetId) {
        this.id = id;
        this.libelle = libelle;
        this.priorite = priorite;
        this.dateLimite = dateLimite;
        this.etat = etat;
        this.projetId = projetId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getPriorite() {
        return priorite;
    }

    public void setPriorite(String priorite) {
        this.priorite = priorite;
    }

    public LocalDate getDateLimite() {
        return dateLimite;
    }

    public void setDateLimite(LocalDate dateLimite) {
        this.dateLimite = dateLimite;
    }

    public String getEtat() {
        return etat;
    }

    public void setEtat(String etat) {
        this.etat = etat;
    }

    public int getProjetId() {
        return projetId;
    }

    public void setProjetId(int projetId) {
        this.projetId = projetId;
    }
}
