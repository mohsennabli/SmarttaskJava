package com.smarttask.controller;

import com.smarttask.model.CalendarDay;
import com.smarttask.model.Ticket;
import com.smarttask.service.CalendarService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CalendarController extends DashboardNavigationController {

    @FXML private Label monthYearLabel;
    @FXML private GridPane calendarGrid;
    @FXML private Button prevMonthBtn;
    @FXML private Button nextMonthBtn;
    @FXML private Button todayBtn;
    @FXML private ComboBox<String> filterStatusCombo;
    @FXML private ComboBox<String> filterPriorityCombo;

    private CalendarService calendarService;
    private int currentYear;
    private int currentMonth;
    private List<Ticket> allTickets;

    @FXML
    public void initialize() {
        initializeDashboardHeader();

        calendarService = new CalendarService();

        // Initialiser avec la date courante
        LocalDate now = LocalDate.now();
        currentYear = now.getYear();
        currentMonth = now.getMonthValue();

        // Configurer les filtres
        setupFilters();

        // Configurer les boutons
        prevMonthBtn.setOnAction(e -> changeMonth(-1));
        nextMonthBtn.setOnAction(e -> changeMonth(1));
        todayBtn.setOnAction(e -> goToToday());

        // Charger le calendrier
        refreshCalendar();
    }

    private void setupFilters() {
        filterStatusCombo.getItems().addAll("Tous", "open", "in_progress", "resolved", "closed");
        filterStatusCombo.setValue("Tous");
        filterPriorityCombo.getItems().addAll("Tous", "low", "medium", "high", "urgent");
        filterPriorityCombo.setValue("Tous");

        filterStatusCombo.valueProperty().addListener((obs, oldVal, newVal) -> refreshCalendar());
        filterPriorityCombo.valueProperty().addListener((obs, oldVal, newVal) -> refreshCalendar());
    }

    private void changeMonth(int delta) {
        YearMonth yearMonth = YearMonth.of(currentYear, currentMonth).plusMonths(delta);
        currentYear = yearMonth.getYear();
        currentMonth = yearMonth.getMonthValue();
        refreshCalendar();
    }

    private void goToToday() {
        LocalDate today = LocalDate.now();
        currentYear = today.getYear();
        currentMonth = today.getMonthValue();
        refreshCalendar();
    }

    private void refreshCalendar() {
        // Mettre à jour le titre
        YearMonth yearMonth = YearMonth.of(currentYear, currentMonth);
        monthYearLabel.setText(yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));

        // Générer les jours du mois
        List<CalendarDay> days = calendarService.generateMonthDays(currentYear, currentMonth);

        // Nettoyer la grille
        calendarGrid.getChildren().clear();

        // Ajouter les en-têtes des jours
        List<String> dayNames = calendarService.getDayNames();
        for (int i = 0; i < dayNames.size(); i++) {
            Label dayLabel = new Label(dayNames.get(i));
            dayLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #a0aec0; -fx-padding: 10;");
            dayLabel.setAlignment(Pos.CENTER);
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            calendarGrid.add(dayLabel, i, 0);
        }

        // Ajouter les cellules des jours
        int row = 1;
        int col = 0;
        for (CalendarDay day : days) {
            VBox cell = createDayCell(day);
            calendarGrid.add(cell, col, row);

            col++;
            if (col >= 7) {
                col = 0;
                row++;
            }
        }
    }

    private VBox createDayCell(CalendarDay day) {
        VBox cell = new VBox(5);
        cell.setPadding(new Insets(8));
        cell.setStyle(getCellStyle(day));
        cell.setMinHeight(100);
        cell.setMinWidth(100);

        // Jour du mois
        Label dayNumber = new Label(String.valueOf(day.getDate().getDayOfMonth()));
        dayNumber.setStyle(getDayNumberStyle(day));

        // Nombre de tickets
        Label ticketCount = new Label(day.getTicketsSummary());
        ticketCount.setStyle("-fx-font-size: 11px; -fx-text-fill: #718096;");

        cell.getChildren().addAll(dayNumber, ticketCount);

        // Ajouter les tickets (max 3)
        int maxDisplay = Math.min(day.getTickets().size(), 3);
        for (int i = 0; i < maxDisplay; i++) {
            Ticket ticket = day.getTickets().get(i);
            HBox ticketBox = createTicketBox(ticket);
            cell.getChildren().add(ticketBox);
        }

        // Indiquer plus de tickets
        if (day.getTickets().size() > 3) {
            Label moreLabel = new Label("+ " + (day.getTickets().size() - 3) + " autres");
            moreLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #718096; -fx-font-style: italic;");
            cell.getChildren().add(moreLabel);
        }

        // Click sur la cellule
        cell.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                showDayTickets(day);
            }
        });

        return cell;
    }

    private HBox createTicketBox(Ticket ticket) {
        HBox box = new HBox(5);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-background-color: " + calendarService.getPriorityColor(ticket.getPriorite()) + "20; " +
                "-fx-background-radius: 5; -fx-padding: 4; -fx-cursor: hand;");

        Label icon = new Label(calendarService.getStatusIcon(ticket.getStatut()));
        icon.setStyle("-fx-font-size: 12px;");

        Label title = new Label(ticket.getTitre());
        title.setStyle("-fx-font-size: 11px; -fx-text-fill: #2d3748;");
        title.setMaxWidth(80);
        title.setWrapText(true);

        box.getChildren().addAll(icon, title);

        // Double-clic pour ouvrir le ticket
        box.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                openTicket(ticket);
            }
        });

        return box;
    }

    private String getCellStyle(CalendarDay day) {
        String style = "-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-width: 0.5;";
        if (!day.isCurrentMonth()) {
            style = "-fx-background-color: #f7fafc; -fx-border-color: #e2e8f0; -fx-border-width: 0.5;";
        }
        if (day.isToday()) {
            style = "-fx-background-color: #ebf8ff; -fx-border-color: #4299e1; -fx-border-width: 2;";
        }
        return style;
    }

    private String getDayNumberStyle(CalendarDay day) {
        String style = "-fx-font-weight: bold; -fx-font-size: 14px;";
        if (!day.isCurrentMonth()) {
            style += "-fx-text-fill: #cbd5e0;";
        } else if (day.isToday()) {
            style += "-fx-text-fill: #4299e1;";
        } else {
            style += "-fx-text-fill: #2d3748;";
        }
        return style;
    }

    private void showDayTickets(CalendarDay day) {
        if (day.getTickets().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Aucun ticket");
            alert.setHeaderText(null);
            alert.setContentText("Aucun ticket pour le " + day.getDate().toString());
            alert.showAndWait();
        } else {
            StringBuilder message = new StringBuilder("Tickets du " + day.getDate().toString() + " :\n\n");
            for (Ticket ticket : day.getTickets()) {
                message.append("• #").append(ticket.getId())
                        .append(" - ").append(ticket.getTitre())
                        .append(" (").append(ticket.getStatut()).append(")\n");
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Liste des tickets");
            alert.setHeaderText(null);
            alert.setContentText(message.toString());
            alert.showAndWait();
        }
    }

    private void openTicket(Ticket ticket) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/smarttask/ticket.fxml")
            );
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Ticket #" + ticket.getId() + " - " + ticket.getTitre());
            stage.setScene(new javafx.scene.Scene(root, 1200, 800));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}