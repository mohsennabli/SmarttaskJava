package com.smarttask.service;

import com.smarttask.dao.TicketDAO;
import com.smarttask.model.CalendarDay;
import com.smarttask.model.Ticket;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class CalendarService {

    private TicketDAO ticketDAO;

    public CalendarService() {
        this.ticketDAO = new TicketDAO();
    }

    /**
     * Récupère les tickets pour une période donnée
     */
    public List<Ticket> getTicketsForPeriod(LocalDate startDate, LocalDate endDate) {
        List<Ticket> allTickets = ticketDAO.getAll();
        return allTickets.stream()
                .filter(ticket -> {
                    LocalDate ticketDate = ticket.getDateCreation().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                    return !ticketDate.isBefore(startDate) && !ticketDate.isAfter(endDate);
                })
                .collect(Collectors.toList());
    }

    /**
     * Génère les jours du mois pour le calendrier
     */
    public List<CalendarDay> generateMonthDays(int year, int month) {
        List<CalendarDay> days = new ArrayList<>();
        YearMonth yearMonth = YearMonth.of(year, month);

        // Premier jour du mois
        LocalDate firstOfMonth = yearMonth.atDay(1);
        // Dernier jour du mois
        LocalDate lastOfMonth = yearMonth.atEndOfMonth();

        // Jour de semaine du premier jour (1 = Lundi, 7 = Dimanche)
        int startOffset = firstOfMonth.getDayOfWeek().getValue() - 1;

        // Récupérer le premier jour à afficher (commence par Lundi)
        LocalDate startDate = firstOfMonth.minusDays(startOffset);

        // Récupérer les tickets du mois
        List<Ticket> tickets = getTicketsForPeriod(firstOfMonth, lastOfMonth);

        // Générer 42 jours (6 semaines)
        for (int i = 0; i < 42; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            CalendarDay day = new CalendarDay(currentDate);

            // Vérifier si le jour est dans le mois courant
            day.setCurrentMonth(currentDate.getMonthValue() == month);

            // Vérifier si c'est aujourd'hui
            day.setToday(currentDate.equals(LocalDate.now()));

            // Ajouter les tickets de ce jour
            for (Ticket ticket : tickets) {
                LocalDate ticketDate = ticket.getDateCreation().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                if (ticketDate.equals(currentDate)) {
                    day.addTicket(ticket);
                }
            }

            days.add(day);
        }

        return days;
    }

    /**
     * Récupère les noms des jours
     */
    public List<String> getDayNames() {
        return Arrays.asList("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim");
    }

    /**
     * Obtenir la couleur selon le nombre de tickets
     */
    public String getTicketColor(int count) {
        if (count == 0) return "#4a5568";      // Gris foncé
        if (count <= 2) return "#48bb78";     // Vert
        if (count <= 5) return "#ecc94b";     // Jaune
        return "#f56565";                      // Rouge
    }

    /**
     * Obtenir la couleur selon la priorité du ticket
     */
    public String getPriorityColor(String priorite) {
        switch (priorite) {
            case "low": return "#48bb78";      // Vert
            case "medium": return "#ecc94b";   // Jaune
            case "high": return "#ed8936";     // Orange
            case "urgent": return "#f56565";   // Rouge
            default: return "#a0aec0";         // Gris
        }
    }

    /**
     * Obtenir l'icône selon le statut
     */
    public String getStatusIcon(String statut) {
        switch (statut) {
            case "open": return "🟡";
            case "in_progress": return "🔄";
            case "resolved": return "✅";
            case "closed": return "🔘";
            default: return "📋";
        }
    }
}