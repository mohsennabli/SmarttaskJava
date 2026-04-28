package com.smarttask.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CalendarDay {

    private LocalDate date;
    private List<Ticket> tickets;
    private boolean isCurrentMonth;
    private boolean isToday;

    public CalendarDay(LocalDate date) {
        this.date = date;
        this.tickets = new ArrayList<>();
        this.isCurrentMonth = true;
        this.isToday = false;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public List<Ticket> getTickets() { return tickets; }
    public void setTickets(List<Ticket> tickets) { this.tickets = tickets; }
    public void addTicket(Ticket ticket) { this.tickets.add(ticket); }

    public boolean isCurrentMonth() { return isCurrentMonth; }
    public void setCurrentMonth(boolean currentMonth) { isCurrentMonth = currentMonth; }

    public boolean isToday() { return isToday; }
    public void setToday(boolean today) { isToday = today; }

    public int getTicketCount() { return tickets.size(); }

    public String getTicketsSummary() {
        if (tickets.isEmpty()) return "Aucun ticket";
        if (tickets.size() == 1) return "1 ticket";
        return tickets.size() + " tickets";
    }
}