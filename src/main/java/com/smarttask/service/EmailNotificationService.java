package com.smarttask.service;

import com.smarttask.util.EnvConfig;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.net.URI;
import java.util.Properties;

public class EmailNotificationService {

    private final String smtpHost;
    private final int smtpPort;
    private final String smtpUser;
    private final String smtpPassword;
    private final String fromEmail;
    private final boolean useAuth;
    private final boolean startTls;

    public EmailNotificationService() {
        MailConfig config = resolveMailConfig();
        this.smtpHost = config.host;
        this.smtpPort = config.port;
        this.smtpUser = config.user;
        this.smtpPassword = config.password;
        this.useAuth = config.auth;
        this.startTls = config.startTls;
        this.fromEmail = EnvConfig.read("MAILER_FROM", EnvConfig.read("SMARTTASK_MAIL_FROM", "no-reply@smarttask.local"));
    }

    public void sendPlainText(String to, String subject, String body) throws MessagingException {
        if (to == null || to.isBlank()) {
            return;
        }
        Session session = Session.getInstance(buildProperties(), buildAuthenticator());
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromEmail));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject == null ? "SmartTask Notification" : subject);
        message.setText(body == null ? "" : body);
        Transport.send(message);
    }

    private Properties buildProperties() {
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", String.valueOf(smtpPort));
        props.put("mail.smtp.auth", String.valueOf(useAuth));
        props.put("mail.smtp.starttls.enable", String.valueOf(startTls));
        return props;
    }

    private Authenticator buildAuthenticator() {
        if (!useAuth) {
            return null;
        }
        return new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUser, smtpPassword);
            }
        };
    }

    private static MailConfig resolveMailConfig() {
        String dsn = EnvConfig.readPreferFile("MAILER_DSN", "");
        if (!dsn.isBlank()) {
            MailConfig fromDsn = parseMailerDsn(dsn);
            if (fromDsn != null) {
                return fromDsn;
            }
        }

        String host = EnvConfig.readPreferFile("SMARTTASK_SMTP_HOST", "127.0.0.1");
        int port = EnvConfig.readInt("SMARTTASK_SMTP_PORT", 1025);
        String user = EnvConfig.readPreferFile("SMARTTASK_SMTP_USER", "");
        String pass = EnvConfig.readPreferFile("SMARTTASK_SMTP_PASSWORD", "");
        boolean auth = EnvConfig.readBoolean("SMARTTASK_SMTP_AUTH", false);
        boolean tls = EnvConfig.readBoolean("SMARTTASK_SMTP_STARTTLS", false);
        return new MailConfig(host, port, user, pass, auth, tls);
    }

    private static MailConfig parseMailerDsn(String dsn) {
        try {
            URI uri = URI.create(dsn);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
            if (!"smtp".equals(scheme) && !"smtps".equals(scheme)) {
                return null;
            }
            String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : ("smtps".equals(scheme) ? 465 : 25);
            String user = "";
            String pass = "";
            String userInfo = uri.getUserInfo();
            if (userInfo != null && !userInfo.isBlank()) {
                int sep = userInfo.indexOf(':');
                if (sep >= 0) {
                    user = userInfo.substring(0, sep);
                    pass = userInfo.substring(sep + 1);
                } else {
                    user = userInfo;
                }
            }

            String query = uri.getQuery() == null ? "" : uri.getQuery();
            boolean tls = query.contains("encryption=tls") || query.contains("encryption=starttls");
            boolean auth = query.contains("auth_mode=") || !user.isBlank();
            return new MailConfig(host, port, user, pass, auth, tls);
        } catch (Exception ex) {
            return null;
        }
    }

    private record MailConfig(String host, int port, String user, String password, boolean auth, boolean startTls) {
    }
}
