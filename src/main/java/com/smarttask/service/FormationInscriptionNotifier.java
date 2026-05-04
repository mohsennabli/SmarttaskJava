package com.smarttask.service;

import com.smarttask.model.Formation;
import com.smarttask.model.User;
import jakarta.mail.MessagingException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FormationInscriptionNotifier {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmailNotificationService emailService = new EmailNotificationService();
    private final GoogleCalendarService calendarService = new GoogleCalendarService();

    public void sendInscriptionConfirmation(User user, Formation formation, String statut) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank() || formation == null) {
            return;
        }

        String calendarUrl = calendarService.buildEventUrl(formation);
        String body = "Bonjour " + safe(user.getName(), user.getEmail()) + ",\n\n"
                + "Votre inscription a la formation \"" + safe(formation.getTitre(), "Formation") + "\" a bien ete enregistree.\n\n"
                + "Date de debut : " + fmt(formation.getDateDebut()) + "\n"
                + "Date de fin : " + fmt(formation.getDateFin()) + "\n"
                + "Statut : " + safe(statut, "en_cours") + "\n\n"
                + (calendarUrl.isBlank() ? "" : "Ajouter dans Google Calendar : " + calendarUrl + "\n\n")
                + "Merci d'utiliser SmartTask.";

        try {
            emailService.sendPlainText(
                    user.getEmail(),
                    "Confirmation d'inscription - " + safe(formation.getTitre(), "Formation"),
                    body
            );
        } catch (MessagingException ex) {
            System.err.println("Confirmation email failed: " + ex.getMessage());
        }
    }

    public void sendCertificateIssued(User user, Formation formation) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank() || formation == null) {
            return;
        }
        String body = "Bonjour " + safe(user.getName(), user.getEmail()) + ",\n\n"
                + "Votre certificat est disponible pour la formation \"" + safe(formation.getTitre(), "Formation") + "\".\n"
                + "Date de completion : " + LocalDate.now().format(DF) + "\n\n"
                + "Felicitations.";
        try {
            emailService.sendPlainText(
                    user.getEmail(),
                    "Certificat disponible - " + safe(formation.getTitre(), "Formation"),
                    body
            );
        } catch (MessagingException ex) {
            System.err.println("Certificate email failed: " + ex.getMessage());
        }
    }

    public void openFormationInGoogleCalendar(Formation formation) {
        calendarService.openEventInBrowser(formation);
    }

    private static String fmt(LocalDate d) {
        return d == null ? "-" : d.format(DF);
    }

    private static String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }
}
