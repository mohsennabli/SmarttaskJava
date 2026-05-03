package com.smarttask.dao;

import com.smarttask.model.Projet;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroqTaskRecommendationService {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();
    private static final String GROQ_API_KEY = getenv("GROQ_API_KEY", "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");
    private static final String GROQ_MODEL = getenv("GROQ_MODEL", "llama-3.1-70b-versatile");
    private static final URI ENDPOINT = URI.create("https://api.groq.com/openai/v1/chat/completions");
    private static final Pattern CONTENT_PATTERN = Pattern.compile("\"content\"\\s*:\\s*\"((?:\\\\.|[^\\\"\\\\])*)\"", Pattern.DOTALL);

    public List<ProjectTaskSuggestion> recommendTasks(Projet projet) {
        try {
            String prompt = buildPrompt(projet);
            String requestBody = "{" +
                    "\"model\":\"" + escapeJson(GROQ_MODEL) + "\"," +
                    "\"messages\":[{" +
                    "\"role\":\"system\",\"content\":\"You generate concise project task plans.\"},{" +
                    "\"role\":\"user\",\"content\":\"" + escapeJson(prompt) + "\"}]," +
                    "\"temperature\":0.6," +
                    "\"max_tokens\":600" +
                    "}";

            HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
                    .header("Authorization", "Bearer " + GROQ_API_KEY)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return fallbackSuggestions(projet);
            }

            String content = extractContent(response.body());
            List<ProjectTaskSuggestion> suggestions = parseSuggestions(content);
            return suggestions.isEmpty() ? fallbackSuggestions(projet) : suggestions;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fallbackSuggestions(projet);
        } catch (Exception e) {
            return fallbackSuggestions(projet);
        }
    }

    private String buildPrompt(Projet projet) {
        return "Analyse ce projet et propose 4 a 6 taches concretes. "
                + "Retourne uniquement des lignes au format exact: libelle ; priorite ; delai_jours ; raison. "
                + "Priorite doit etre basse, moyenne ou haute. Delai_jours doit etre un entier positif. "
                + "Projet: " + projet.getNom() + ". Description: " + projet.getDescription();
    }

    private String extractContent(String responseBody) {
        Matcher matcher = CONTENT_PATTERN.matcher(responseBody);
        if (!matcher.find()) {
            return "";
        }
        return unescapeJsonString(matcher.group(1));
    }

    private List<ProjectTaskSuggestion> parseSuggestions(String content) {
        List<ProjectTaskSuggestion> suggestions = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return suggestions;
        }

        String[] lines = content.split("\\R");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            line = line.replaceFirst("^[0-9]+[.)]\\s*", "");
            line = line.replaceFirst("^[-*•]\\s*", "");

            String[] parts = line.split("\\s*;\\s*");
            if (parts.length < 4) {
                continue;
            }

            String libelle = parts[0].trim();
            String priorite = normalizePriority(parts[1].trim());
            int delayDays = parseDelay(parts[2].trim());
            String reason = parts[3].trim();

            if (!libelle.isEmpty()) {
                suggestions.add(new ProjectTaskSuggestion(libelle, priorite, delayDays, reason));
            }
        }
        return suggestions;
    }

    private List<ProjectTaskSuggestion> fallbackSuggestions(Projet projet) {
        List<ProjectTaskSuggestion> suggestions = new ArrayList<>();
        String text = (projet.getNom() + " " + projet.getDescription()).toLowerCase(Locale.ROOT);

        suggestions.add(new ProjectTaskSuggestion("Cadrage et planning", "haute", 3, "Definir le perimetre et le calendrier du projet."));
        suggestions.add(new ProjectTaskSuggestion("Conception fonctionnelle", "haute", 6, "Formaliser les besoins et les livrables attendus."));
        suggestions.add(new ProjectTaskSuggestion("Mise en oeuvre technique", text.contains("design") ? "moyenne" : "haute", 10, "Executer la partie principale du projet."));
        suggestions.add(new ProjectTaskSuggestion("Tests et validation", "moyenne", 14, "Verifier la qualite et valider les resultats."));
        suggestions.add(new ProjectTaskSuggestion("Mise en production", "basse", 18, "Preparer la livraison finale."));
        return suggestions;
    }

    private String normalizePriority(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("haute")) {
            return "haute";
        }
        if (normalized.contains("basse")) {
            return "basse";
        }
        return "moyenne";
    }

    private int parseDelay(String value) {
        try {
            return Math.max(1, Integer.parseInt(value.replaceAll("[^0-9]", "")));
        } catch (Exception e) {
            return 7;
        }
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private String unescapeJsonString(String value) {
        return value.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\/", "/")
                .replace("\\\\", "\\");
    }

    private static String getenv(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            value = System.getProperty(name);
        }
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
