package com.smarttask.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Inscription joined with formation fields for the enrollments table view.
 */
public class InscriptionRow {
    private int inscriptionId;
    private int formationId;
    private String formationTitre;
    private LocalDate formationDateDebut;
    private LocalDate formationDateFin;
    private LocalDateTime dateInscription;
    private String statut;
    private int progression;
    private boolean certificat;

    public int getInscriptionId() {
        return inscriptionId;
    }

    public void setInscriptionId(int inscriptionId) {
        this.inscriptionId = inscriptionId;
    }

    public int getFormationId() {
        return formationId;
    }

    public void setFormationId(int formationId) {
        this.formationId = formationId;
    }

    public String getFormationTitre() {
        return formationTitre;
    }

    public void setFormationTitre(String formationTitre) {
        this.formationTitre = formationTitre;
    }

    public LocalDate getFormationDateDebut() {
        return formationDateDebut;
    }

    public void setFormationDateDebut(LocalDate formationDateDebut) {
        this.formationDateDebut = formationDateDebut;
    }

    public LocalDate getFormationDateFin() {
        return formationDateFin;
    }

    public void setFormationDateFin(LocalDate formationDateFin) {
        this.formationDateFin = formationDateFin;
    }

    public LocalDateTime getDateInscription() {
        return dateInscription;
    }

    public void setDateInscription(LocalDateTime dateInscription) {
        this.dateInscription = dateInscription;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public int getProgression() {
        return progression;
    }

    public void setProgression(int progression) {
        this.progression = progression;
    }

    public boolean isCertificat() {
        return certificat;
    }

    public void setCertificat(boolean certificat) {
        this.certificat = certificat;
    }
}
