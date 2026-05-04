package com.smarttask.service;

import com.smarttask.dao.UserDAO;
import com.smarttask.model.User;
import com.smarttask.util.InputValidator;
import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

public class ForgotPasswordService {
    private static final int RESET_TOKEN_VALIDITY_MINUTES = 30;

    private final UserDAO userDAO = new UserDAO();
    private final EmailService emailService = new EmailService();

    public boolean requestPasswordReset(String email) {
        String normalizedEmail = InputValidator.sanitize(email);
        if (normalizedEmail.isEmpty() || !InputValidator.isValidEmail(normalizedEmail)) {
            return false;
        }

        User user = userDAO.findByEmail(normalizedEmail);
        if (user == null) {
            return true;
        }

        if (!emailService.isConfigured()) {
            System.err.println("Password reset requested, but SMTP is not configured.");
            return false;
        }

        String rawToken = generateResetToken();
        String hashedToken = hashToken(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(RESET_TOKEN_VALIDITY_MINUTES);

        if (!userDAO.storeResetToken(user.getIduser(), hashedToken, expiresAt)) {
            return false;
        }

        return emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), rawToken, expiresAt);
    }

    public boolean resetPassword(String email, String token, String newPassword) {
        String normalizedEmail = InputValidator.sanitize(email);
        String normalizedToken = InputValidator.sanitize(token);
        String normalizedPassword = InputValidator.sanitize(newPassword);

        if (normalizedEmail.isEmpty() || !InputValidator.isValidEmail(normalizedEmail)) {
            return false;
        }
        if (normalizedToken.isEmpty() || normalizedPassword.isEmpty()) {
            return false;
        }
        if (!InputValidator.isValidPassword(normalizedPassword)) {
            return false;
        }

        User user = userDAO.findByEmail(normalizedEmail);
        if (user == null) {
            return false;
        }

        if (user.getResetToken() == null || user.getResetTokenExpiresAt() == null) {
            return false;
        }
        if (user.getResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }

        String providedTokenHash = hashToken(normalizedToken);
        if (!MessageDigest.isEqual(user.getResetToken().getBytes(StandardCharsets.UTF_8),
                providedTokenHash.getBytes(StandardCharsets.UTF_8))) {
            return false;
        }

        String hashedPassword = BCrypt.hashpw(normalizedPassword, BCrypt.gensalt());
        return userDAO.updatePasswordAndClearResetToken(user.getIduser(), hashedPassword);
    }

    private static String generateResetToken() {
        return UUID.randomUUID() + "-" + UUID.randomUUID();
    }

    private static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }
}

