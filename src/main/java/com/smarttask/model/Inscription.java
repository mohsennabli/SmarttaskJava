package com.smarttask.model;

import java.time.LocalDateTime;

public class Inscription {
    private int id;
    private LocalDateTime dateInscription;
    private String statut;
    private int progression;
    private boolean certificat;
    private int userId;
    private int formationId;
    private LocalDateTime absentFollowUpSentAt;

    public Inscription() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getFormationId() {
        return formationId;
    }

    public void setFormationId(int formationId) {
        this.formationId = formationId;
    }

    public LocalDateTime getAbsentFollowUpSentAt() {
        return absentFollowUpSentAt;
    }

    public void setAbsentFollowUpSentAt(LocalDateTime absentFollowUpSentAt) {
        this.absentFollowUpSentAt = absentFollowUpSentAt;
    }
}
