package com.smarttask.service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class EmailService {
    private static final String SMTP_HOST = readConfig("SMARTTASK_SMTP_HOST", "smtp.gmail.com");
    private static final int SMTP_PORT = readIntConfig("SMARTTASK_SMTP_PORT", 465);
    private static final String SMTP_USERNAME = readConfig("SMARTTASK_SMTP_USERNAME", "mohsennabli321@gmail.com");
    private static final String SMTP_PASSWORD = readConfig("SMARTTASK_SMTP_PASSWORD", "Yubjuivfyaehuquda");
    private static final String SMTP_FROM = readConfig("SMARTTASK_SMTP_FROM", SMTP_USERNAME);
    private static final String SMTP_FROM_NAME = readConfig("SMARTTASK_SMTP_FROM_NAME", "SmartTask");
    private static final boolean SMTP_TLS_ENABLED = readBooleanConfig("SMARTTASK_SMTP_TLS_ENABLED", true);
    private static final boolean SMTP_SSL_ENABLED = readBooleanConfig("SMARTTASK_SMTP_SSL_ENABLED", SMTP_PORT == 465);

    public boolean isConfigured() {
        return isConfiguredStatic();
    }

    public boolean sendPasswordResetEmail(String recipientEmail, String recipientName, String resetToken, LocalDateTime expiresAt) {
        if (!isConfiguredStatic()) {
            System.err.println("SMTP is not configured. Set SMARTTASK_SMTP_* keys in your .env or environment variables.");
            return false;
        }

        try {
            Properties properties = new Properties();
            properties.put("mail.smtp.auth", "true");
            boolean useSsl = SMTP_SSL_ENABLED;
            boolean useStartTls = SMTP_TLS_ENABLED && !useSsl;
            properties.put("mail.smtp.starttls.enable", String.valueOf(useStartTls));
            properties.put("mail.smtp.ssl.enable", String.valueOf(useSsl));
            properties.put("mail.smtp.host", SMTP_HOST);
            properties.put("mail.smtp.port", String.valueOf(SMTP_PORT));
            // set sensible network timeouts (milliseconds) to avoid long blocking calls
            properties.put("mail.smtp.connectiontimeout", "10000");
            properties.put("mail.smtp.timeout", "10000");
            properties.put("mail.smtp.writetimeout", "10000");
            // Trust the SMTP host for SSL/TLS connections and require STARTTLS when configured
            properties.put("mail.smtp.ssl.trust", SMTP_HOST);
            properties.put("mail.smtp.starttls.required", String.valueOf(useStartTls));
            properties.put("mail.smtp.charset", StandardCharsets.UTF_8.name());

            Session session = Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SMTP_USERNAME, SMTP_PASSWORD);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SMTP_FROM, SMTP_FROM_NAME, StandardCharsets.UTF_8.name()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail, false));
            message.setSubject("SmartTask password reset", StandardCharsets.UTF_8.name());
            message.setContent(buildHtmlMessage(recipientName, resetToken, expiresAt), "text/html; charset=UTF-8");

            // Use explicit Transport connect/send so we control connect timeout and can log more details
            Transport transport = null;
            try {
                transport = session.getTransport("smtp");
                transport.connect(SMTP_HOST, SMTP_PORT, SMTP_USERNAME, SMTP_PASSWORD);
                transport.sendMessage(message, message.getAllRecipients());
            } finally {
                if (transport != null) {
                    try {
                        transport.close();
                    } catch (MessagingException ignored) {
                    }
                }
            }
            return true;
        } catch (MessagingException e) {
            System.err.println("Failed to send password reset email: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("Unexpected email error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static String buildHtmlMessage(String recipientName, String resetToken, LocalDateTime expiresAt) {
        String safeName = (recipientName == null || recipientName.isBlank()) ? "there" : recipientName;
        String expiryText = expiresAt == null
                ? "30 minutes"
                : expiresAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                </head>
                <body style="font-family: Arial, sans-serif; background-color: #f8fafc; color: #111827; padding: 24px;">
                    <div style="max-width: 560px; margin: 0 auto; background: #ffffff; border: 1px solid #e5e7eb; border-radius: 12px; padding: 24px;">
                        <h2 style="margin-top: 0; color: #111827;">SmartTask password reset</h2>
                        <p>Hello %s,</p>
                        <p>We received a request to reset your SmartTask password.</p>
                        <p>Your one-time reset code is:</p>
                        <div style="font-size: 22px; font-weight: bold; letter-spacing: 1px; padding: 14px 16px; background: #f3f4f6; border: 1px dashed #9ca3af; border-radius: 10px; margin: 18px 0;">
                            %s
                        </div>
                        <p>This code expires at <strong>%s</strong>.</p>
                        <p>Open SmartTask, go to <strong>Forgot Password?</strong>, then paste this code into the reset section to choose a new password.</p>
                        <p style="color: #6b7280; font-size: 12px;">If you did not request this reset, you can safely ignore this email.</p>
                    </div>
                </body>
                </html>
                """.formatted(escapeHtml(safeName), escapeHtml(resetToken), escapeHtml(expiryText));
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static boolean isConfiguredStatic() {
        return SMTP_HOST != null && !SMTP_HOST.isBlank() && !SMTP_HOST.startsWith("YOUR_")
                && SMTP_USERNAME != null && !SMTP_USERNAME.isBlank() && !SMTP_USERNAME.startsWith("YOUR_")
                && SMTP_PASSWORD != null && !SMTP_PASSWORD.isBlank() && !SMTP_PASSWORD.startsWith("YOUR_");
    }

    private static String readConfig(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.isBlank()) {
            return value;
        }

        value = System.getProperty(key);
        if (value != null && !value.isBlank()) {
            return value;
        }

        value = System.getProperty(key.toLowerCase().replace('_', '.'));
        if (value != null && !value.isBlank()) {
            return value;
        }

        // If caller asked for SMARTTASK_SMTP_* keys but the .env uses mail.smtp.* keys,
        // try alternative forms so both `SMARTTASK_SMTP_*` and `mail.smtp.*` .env styles work.
        if (key != null && key.startsWith("SMARTTASK_")) {
            String alt = key.substring("SMARTTASK_".length()).toLowerCase().replace('_', '.');
            // try plain alt (e.g. smtp.username)
            value = System.getenv(alt);
            if (value != null && !value.isBlank()) return value;
            value = System.getProperty(alt);
            if (value != null && !value.isBlank()) return value;
            // try mail.<alt> (e.g. mail.smtp.username) which is common in .env
            String mailAlt = "mail." + alt;
            value = System.getenv(mailAlt);
            if (value != null && !value.isBlank()) return value;
            value = System.getProperty(mailAlt);
            if (value != null && !value.isBlank()) return value;
        }

        return defaultValue;
    }

    private static int readIntConfig(String key, int defaultValue) {
        String raw = readConfig(key, null);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean readBooleanConfig(String key, boolean defaultValue) {
        String raw = readConfig(key, null);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(raw.trim());
    }
}





