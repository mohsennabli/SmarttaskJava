package com.smarttask.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Utility to load environment variables from .env file.
 * Loads .env from the project root directory and sets them as system properties.
 */
public class EnvLoader {
    private static final String ENV_FILE = ".env";
    private static boolean loaded = false;

    /**
     * Load environment variables from .env file if it exists.
     * System properties take precedence over .env values.
     */
    public static void load() {
        if (loaded) {
            return;
        }
        
        Path envPath = Paths.get(ENV_FILE);
        if (!Files.exists(envPath)) {
            System.out.println("No .env file found at: " + envPath.toAbsolutePath());
            loaded = true;
            return;
        }

        try (Stream<String> lines = Files.lines(envPath)) {
            lines.filter(line -> !line.trim().isEmpty() && !line.trim().startsWith("#"))
                    .forEach(line -> {
                        String[] parts = line.split("=", 2);
                        if (parts.length == 2) {
                            String key = parts[0].trim();
                            String value = parts[1].trim();
                            
                            // Remove quotes if present
                            if (value.startsWith("\"") && value.endsWith("\"")) {
                                value = value.substring(1, value.length() - 1);
                            }
                            
                            // Only set if not already set as system property or environment variable
                            if (System.getProperty(key) == null && System.getenv(key) == null) {
                                System.setProperty(key, value);
                            }
                        }
                    });
            System.out.println(".env file loaded successfully from: " + envPath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error loading .env file: " + e.getMessage());
        }
        
        loaded = true;
    }
}

