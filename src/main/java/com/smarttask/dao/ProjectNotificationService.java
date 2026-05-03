package com.smarttask.dao;

import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import com.smarttask.model.Projet;
import com.smarttask.model.Tache;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class ProjectNotificationService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRANCE);
    private static final String SMTP_HOST = getenv("SMTP_HOST", "smtp.gmail.com");
    private static final String SMTP_PORT = getenv("SMTP_PORT", "587");
    private static final String SMTP_USER = getenv("SMTP_USER", "karouihajer5@gmail.com");
    private static final String SMTP_PASSWORD = getenv("SMTP_PASSWORD", "ppkztvxkinrmwqsd");
    private static final String FROM_NAME = getenv("SMTP_FROM_NAME", "Gestion Projet");

    public void notifyCreated(Projet projet, List<Tache> approvedTasks) {
        sendToAllUsers(
                "Nouveau projet cree: " + projet.getNom(),
                buildHtml(projet, "Projet cree", "Un nouveau projet est disponible", approvedTasks, accentColor("created")));
    }
    //test

    public void notifyUpdated(Projet projet) {
        sendToAllUsers(
                "Projet modifie: " + projet.getNom(),
                buildHtml(projet, "Projet modifie", "Un projet a ete mis a jour", List.of(), accentColor("updated")));
    }

    public void notifyDeleted(Projet projet) {
        sendToAllUsers(
                "Projet supprime: " + projet.getNom(),
                buildHtml(projet, "Projet supprime", "Un projet a ete supprime", List.of(), accentColor("deleted")));
    }

    private void sendToAllUsers(String subject, String htmlBody) {
        Session session = createSession();
        sendEmail(session, "karouihajer5@gmail.com", "ismail karoui", subject, htmlBody);
    }

    private void sendEmail(Session session, String email, String fullName, String subject, String htmlBody) {
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SMTP_USER, FROM_NAME));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(email, fullName == null || fullName.isBlank() ? email : fullName));
            message.setSubject(subject, "UTF-8");
            message.setContent(htmlBody, "text/html; charset=UTF-8");
            Transport.send(message);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send notification email", e);
        }
    }

    private Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.trust", SMTP_HOST);

        return Session.getInstance(props, new jakarta.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USER, SMTP_PASSWORD);
            }
        });
    }

    private String buildHtml(Projet projet, String actionLabel, String headline, List<Tache> approvedTasks, String accentColor) {
        StringBuilder tasksHtml = new StringBuilder();
        if (approvedTasks != null && !approvedTasks.isEmpty()) {
            tasksHtml.append("<div style='margin-top:18px;padding-top:16px;border-top:1px solid #e5e7eb;'>");
            tasksHtml.append("<h3 style='margin:0 0 12px;font-size:16px;color:#1d3b53;'>Taches approuvees</h3>");
            for (Tache tache : approvedTasks) {
                tasksHtml.append("<div style='margin-bottom:10px;padding:12px 14px;background:#f8fafc;border:1px solid #e5e7eb;border-radius:12px;'>")
                        .append("<div style='font-weight:700;color:#111827;margin-bottom:4px;'>")
                        .append(escapeHtml(tache.getLibelle()))
                        .append("</div>")
                        .append("<div style='font-size:12px;color:#6b7280;'>Priorite: ")
                        .append(escapeHtml(tache.getPriorite()))
                        .append(" | Echeance: ")
                        .append(tache.getDateLimite() == null ? "-" : tache.getDateLimite().format(DATE_FORMATTER))
                        .append("</div>")
                        .append("</div>");
            }
            tasksHtml.append("</div>");
        }

        return "<!DOCTYPE html>"
                + "<html><head><meta charset='UTF-8'></head><body style='margin:0;padding:0;background:#eef4fb;font-family:Arial,Helvetica,sans-serif;'>"
                + "<div style='max-width:720px;margin:0 auto;padding:32px 18px;'>"
                + "<div style='overflow:hidden;border-radius:24px;background:#ffffff;box-shadow:0 20px 60px rgba(13,38,76,0.14);border:1px solid #dbe4ee;'>"
                + "<div style='padding:28px 30px;background:linear-gradient(135deg," + accentColor + ",#0f172a);color:#fff;'>"
                + "<div style='font-size:12px;letter-spacing:1.5px;text-transform:uppercase;opacity:.88;'>" + escapeHtml(actionLabel) + "</div>"
                + "<h1 style='margin:8px 0 0;font-size:28px;line-height:1.15;'>" + escapeHtml(headline) + "</h1>"
                + "<p style='margin:12px 0 0;font-size:14px;line-height:1.7;opacity:.92;'>Le projet <strong>" + escapeHtml(projet.getNom()) + "</strong> a ete traite dans l'application.</p>"
                + "</div>"
                + "<div style='padding:28px 30px;color:#1f2937;'>"
                + "<div style='display:grid;grid-template-columns:1fr 1fr;gap:14px;'>"
                + card("Projet", projet.getNom(), "#e8f1fb", "#1d3b53")
                + card("Statut", formatStatut(projet.getStatut()), "#ecfdf5", "#047857")
                + card("Debut", projet.getDateDebut() == null ? "-" : projet.getDateDebut().format(DATE_FORMATTER), "#fef3c7", "#b45309")
                + card("Echeance", projet.getDateEcheance() == null ? "-" : projet.getDateEcheance().format(DATE_FORMATTER), "#f3e8ff", "#7c3aed")
                + "</div>"
                + "<div style='margin-top:18px;padding:16px 18px;background:#f8fafc;border:1px solid #e5e7eb;border-radius:16px;'>"
                + "<div style='font-size:13px;color:#6b7280;text-transform:uppercase;letter-spacing:1px;margin-bottom:6px;'>Resume</div>"
                + "<div style='font-size:15px;line-height:1.8;color:#111827;'>" + escapeHtml(projet.getDescription()) + "</div>"
                + "</div>"
                + tasksHtml
                + "<div style='margin-top:22px;padding:18px;border-radius:16px;background:linear-gradient(135deg,#f8fafc,#eef4fb);border:1px solid #dbe4ee;font-size:12px;color:#64748b;line-height:1.7;'>"
                + "Cet email a ete genere automatiquement. Repondez a l'application pour consulter les details du projet et les taches associees."
                + "</div>"
                + "</div></div></div></body></html>";
    }

    private String accentColor(String type) {
        return switch (type) {
            case "created" -> "#0f766e";
            case "updated" -> "#066ac9";
            case "deleted" -> "#b91c1c";
            default -> "#1d3b53";
        };
    }

    private String formatStatut(String statut) {
        String value = statut == null ? "" : statut.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "actif" -> "Actif";
            case "termine" -> "Termine";
            default -> "En attente";
        };
    }

    private String card(String label, String value, String background, String foreground) {
        return "<div style='padding:14px 16px;background:" + background + ";border-radius:14px;border:1px solid rgba(0,0,0,0.04);'>"
                + "<div style='font-size:11px;letter-spacing:1px;text-transform:uppercase;color:#64748b;margin-bottom:6px;'>" + escapeHtml(label) + "</div>"
                + "<div style='font-size:15px;font-weight:700;color:" + foreground + ";'>" + escapeHtml(value) + "</div>"
                + "</div>";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String getenv(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            value = System.getProperty(name);
        }
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
