package com.smarttask.util;

import java.util.regex.Pattern;

public final class InputValidator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    private InputValidator() {
        // Utility class.
    }

    public static String sanitize(String value) {
        return value == null ? "" : value.trim();
    }

    public static boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    public static String validateUserCreationFields(String name, String email, String password, String type) {
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || type.isEmpty()) {
            return "Please fill in all fields.";
        }
        if (!isValidEmail(email)) {
            return "Please enter a valid email address.";
        }
        if (!isValidPassword(password)) {
            return "Password must be at least 8 characters and include letters and numbers.";
        }
        return null;
    }

    public static String validateUserUpdateFields(String name, String email, String type) {
        if (name.isEmpty() || email.isEmpty() || type.isEmpty()) {
            return "Name, email, and type are required.";
        }
        if (!isValidEmail(email)) {
            return "Please enter a valid email address.";
        }
        return null;
    }
}


