package com.smarttask.service;

import com.smarttask.model.Ticket;
import java.util.*;
import java.util.stream.Collectors;

public class StatistiquesService {

    /**
     * Compte les tickets par statut
     */
    public Map<String, Integer> countByStatut(List<Ticket> tickets) {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("open", 0);
        stats.put("in_progress", 0);
        stats.put("resolved", 0);
        stats.put("closed", 0);

        for (Ticket t : tickets) {
            String statut = t.getStatut();
            stats.put(statut, stats.getOrDefault(statut, 0) + 1);
        }
        return stats;
    }

    /**
     * Compte les tickets par priorité
     */
    public Map<String, Integer> countByPriorite(List<Ticket> tickets) {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("low", 0);
        stats.put("medium", 0);
        stats.put("high", 0);
        stats.put("urgent", 0);

        for (Ticket t : tickets) {
            String priorite = t.getPriorite();
            stats.put(priorite, stats.getOrDefault(priorite, 0) + 1);
        }
        return stats;
    }

    /**
     * Tickets créés par jour (7 derniers jours)
     */
    public Map<String, Integer> countByDay(List<Ticket> tickets) {
        Map<String, Integer> dailyStats = new LinkedHashMap<>();

        // Initialiser les 7 derniers jours
        Calendar cal = Calendar.getInstance();
        for (int i = 6; i >= 0; i--) {
            Calendar day = (Calendar) cal.clone();
            day.add(Calendar.DAY_OF_MONTH, -i);
            String dateKey = new java.text.SimpleDateFormat("dd/MM").format(day.getTime());
            dailyStats.put(dateKey, 0);
        }

        // Compter les tickets par jour
        for (Ticket t : tickets) {
            if (t.getDateCreation() != null) {
                String dateKey = new java.text.SimpleDateFormat("dd/MM").format(t.getDateCreation());
                if (dailyStats.containsKey(dateKey)) {
                    dailyStats.put(dateKey, dailyStats.get(dateKey) + 1);
                }
            }
        }

        return dailyStats;
    }

    /**
     * Calcule le pourcentage de tickets résolus
     */
    public double getResolutionRate(List<Ticket> tickets) {
        if (tickets.isEmpty()) return 0;
        long resolved = tickets.stream().filter(t -> "resolved".equals(t.getStatut()) || "closed".equals(t.getStatut())).count();
        return (double) resolved / tickets.size() * 100;
    }

    /**
     * Temps moyen de résolution (en heures) - simulation
     */
    public double getAverageResolutionTime(List<Ticket> tickets) {
        // Simulation: retourne une moyenne basée sur le nombre de tickets
        if (tickets.isEmpty()) return 0;
        return 24.0 / (tickets.size() + 1) * 10;
    }

    /**
     * Récupère les données pour diagramme circulaire (statuts)
     */
    public List<StatData> getPieChartData(List<Ticket> tickets) {
        Map<String, Integer> stats = countByStatut(tickets);
        List<StatData> data = new ArrayList<>();

        data.add(new StatData("Ouvert", stats.getOrDefault("open", 0), "#f39c12"));
        data.add(new StatData("En cours", stats.getOrDefault("in_progress", 0), "#3498db"));
        data.add(new StatData("Résolu", stats.getOrDefault("resolved", 0), "#27ae60"));
        data.add(new StatData("Fermé", stats.getOrDefault("closed", 0), "#95a5a6"));

        return data;
    }

    /**
     * Récupère les données pour diagramme à barres (priorités)
     */
    public List<StatData> getBarChartData(List<Ticket> tickets) {
        Map<String, Integer> stats = countByPriorite(tickets);
        List<StatData> data = new ArrayList<>();

        data.add(new StatData("Basse", stats.getOrDefault("low", 0), "#3498db"));
        data.add(new StatData("Moyenne", stats.getOrDefault("medium", 0), "#f39c12"));
        data.add(new StatData("Haute", stats.getOrDefault("high", 0), "#e67e22"));
        data.add(new StatData("Urgente", stats.getOrDefault("urgent", 0), "#e74c3c"));

        return data;
    }

    /**
     * Classe interne pour les données statistiques
     */
    public static class StatData {
        private String label;
        private int value;
        private String color;

        public StatData(String label, int value, String color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }

        public String getLabel() { return label; }
        public int getValue() { return value; }
        public String getColor() { return color; }
    }
}