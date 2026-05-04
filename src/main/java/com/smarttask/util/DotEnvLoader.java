package com.smarttask.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Loads selected SmartTask configuration keys from a local .env file into JVM system properties.
 * This lets the existing env/system-property lookup code work in IDE and Maven runs.
 */
public final class DotEnvLoader {

    private static final String ENV_FILE_NAME = ".env";

    private DotEnvLoader() {
    }

    public static void loadSmartTaskConfig() {
        Path envPath = Path.of(System.getProperty("user.dir"), ENV_FILE_NAME);
        if (!Files.exists(envPath)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(envPath, StandardCharsets.UTF_8);
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isBlank() || line.startsWith("#") || !line.contains("=")) {
                    continue;
                }

                int equalsIndex = line.indexOf('=');
                String key = line.substring(0, equalsIndex).trim();
                String value = line.substring(equalsIndex + 1).trim();

                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }

                if (shouldExposeAsSystemProperty(key)) {
                    if (System.getProperty(key) == null) {
                        System.setProperty(key, value);
                    }
                    if (System.getProperty(toSystemPropertyKey(key)) == null) {
                        System.setProperty(toSystemPropertyKey(key), value);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Unable to read .env file: " + e.getMessage());
        }
    }

    private static boolean shouldExposeAsSystemProperty(String key) {
        if (key == null || key.isBlank()) return false;
        // Keep legacy SMARTTASK_ keys, and also expose common mail.* keys from .env (user provided)
        return key.startsWith("SMARTTASK_") || key.startsWith("mail.") || key.startsWith("MAIL_");
    }

    private static String toSystemPropertyKey(String envKey) {
        if (envKey == null || envKey.isBlank()) {
            return "";
        }
        return envKey.toLowerCase().replace('_', '.');
    }
}


