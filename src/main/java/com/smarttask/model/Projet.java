package com.smarttask.model;

import java.time.LocalDate;

public class Projet {
    private int id;
    private String nom;
    private String description;
    private LocalDate dateDebut;
    private LocalDate dateEcheance;
    private String statut;

    public Projet(int id, String nom, String description,
                  LocalDate dateDebut, LocalDate dateEcheance, String statut) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.dateDebut = dateDebut;
        this.dateEcheance = dateEcheance;
        this.statut = statut;
    }

    public Projet(String nom, String description,
                  LocalDate dateDebut, LocalDate dateEcheance, String statut) {
        this(0, nom, description, dateDebut, dateEcheance, statut);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateEcheance() {
        return dateEcheance;
    }

    public void setDateEcheance(LocalDate dateEcheance) {
        this.dateEcheance = dateEcheance;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    @Override
    public String toString() {
        return nom;
    }
}
