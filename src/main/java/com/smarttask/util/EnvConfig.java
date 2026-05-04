package com.smarttask.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EnvConfig {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{([A-Za-z0-9_]+)}");
    private static final Map<String, String> FILE_ENV = loadEnvFiles();

    private EnvConfig() {
    }

    public static String read(String key, String fallback) {
        String fromSystem = System.getenv(key);
        if (fromSystem != null && !fromSystem.isBlank()) {
            return fromSystem.trim();
        }
        String fromProperty = System.getProperty(key);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty.trim();
        }
        String fromFile = FILE_ENV.get(key);
        if (fromFile != null && !fromFile.isBlank()) {
            return fromFile.trim();
        }
        return fallback;
    }

    public static String readPreferFile(String key, String fallback) {
        String fromFile = FILE_ENV.get(key);
        if (fromFile != null && !fromFile.isBlank()) {
            return fromFile.trim();
        }
        String fromSystem = System.getenv(key);
        if (fromSystem != null && !fromSystem.isBlank()) {
            return fromSystem.trim();
        }
        String fromProperty = System.getProperty(key);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty.trim();
        }
        return fallback;
    }

    public static int readInt(String key, int fallback) {
        String value = read(key, String.valueOf(fallback));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public static boolean readBoolean(String key, boolean fallback) {
        String value = read(key, String.valueOf(fallback));
        return "1".equals(value) || "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
    }

    private static Map<String, String> loadEnvFiles() {
        Map<String, String> values = new HashMap<>();
        for (Path p : candidateFiles()) {
            if (p == null || !Files.exists(p) || !Files.isRegularFile(p)) {
                continue;
            }
            try {
                parseDotEnvFile(values, p);
            } catch (IOException ignored) {
                // Keep startup resilient when env file is absent or unreadable.
            }
        }
        return values;
    }

    private static List<Path> candidateFiles() {
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        Path parent = cwd.getParent();
        return List.of(
                cwd.resolve(".env"),
                cwd.resolve(".env.local"),
                parent != null ? parent.resolve(".env") : null,
                parent != null ? parent.resolve(".env.local") : null,
                parent != null ? parent.resolve("Esprit_PI_3A55_2025_2026_Smarttask-Gestion-Formations").resolve(".env") : null,
                parent != null ? parent.resolve("Esprit_PI_3A55_2025_2026_Smarttask-Gestion-Formations").resolve(".env.local") : null
        );
    }

    private static void parseDotEnvFile(Map<String, String> out, Path path) throws IOException {
        for (String line : Files.readAllLines(path)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int idx = trimmed.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = trimmed.substring(0, idx).trim();
            String value = trimmed.substring(idx + 1).trim();
            if (value.length() >= 2) {
                char first = value.charAt(0);
                char last = value.charAt(value.length() - 1);
                if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                    value = value.substring(1, value.length() - 1);
                }
            }
            value = resolveInlineVariables(value, out);
            out.putIfAbsent(key, value);
        }
    }

    private static String resolveInlineVariables(String value, Map<String, String> resolved) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String current = value;
        for (int i = 0; i < 5; i++) {
            Matcher matcher = VAR_PATTERN.matcher(current);
            StringBuffer sb = new StringBuffer();
            boolean changed = false;
            while (matcher.find()) {
                String key = matcher.group(1);
                String replacement = readVariableValue(key, resolved);
                if (replacement == null) {
                    replacement = matcher.group(0);
                } else {
                    changed = true;
                }
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(sb);
            current = sb.toString();
            if (!changed) {
                break;
            }
        }
        return current;
    }

    private static String readVariableValue(String key, Map<String, String> resolved) {
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        String prop = System.getProperty(key);
        if (prop != null && !prop.isBlank()) {
            return prop.trim();
        }
        String local = resolved.get(key);
        if (local != null && !local.isBlank()) {
            return local.trim();
        }
        return null;
    }
}
