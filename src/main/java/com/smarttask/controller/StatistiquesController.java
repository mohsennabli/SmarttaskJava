package com.smarttask.controller;

import com.smarttask.dao.TicketDAO;
import com.smarttask.model.Ticket;
import com.smarttask.service.StatistiquesService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import java.util.List;

public class StatistiquesController extends DashboardNavigationController {

    @FXML private PieChart statutPieChart;
    @FXML private BarChart<String, Number> prioriteBarChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private Label lblTotalTickets;
    @FXML private Label lblTauxResolution;
    @FXML private Label lblTempsMoyen;
    @FXML private Label lblTicketsOuverts;
    @FXML private Label lblTicketsEnCours;
    @FXML private Label lblTicketsResolus;
    @FXML private Label lblTicketsFermes;

    private List<Ticket> tickets;
    private StatistiquesService service;

    @FXML
    public void initialize() {
        initializeDashboardHeader();

        service = new StatistiquesService();
        
        // Load tickets from database
        try {
            TicketDAO ticketDAO = new TicketDAO();
            tickets = ticketDAO.getAll();
            loadAllStatistics();
        } catch (Exception e) {
            System.err.println("Error loading tickets for statistics: " + e.getMessage());
            e.printStackTrace();
            loadEmptyData();
        }
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
        loadAllStatistics();
    }

    private void loadAllStatistics() {
        if (tickets == null || tickets.isEmpty()) {
            loadEmptyData();
            return;
        }

        // Diagramme circulaire des statuts
        loadPieChart();

        // Diagramme à barres des priorités
        loadBarChart();

        // Labels de statistiques
        loadLabels();
    }

    private void loadPieChart() {
        statutPieChart.getData().clear();

        var data = service.getPieChartData(tickets);

        for (var stat : data) {
            PieChart.Data slice = new PieChart.Data(stat.getLabel(), stat.getValue());
            statutPieChart.getData().add(slice);

            // Ajouter couleur
            slice.getNode().setStyle("-fx-pie-color: " + stat.getColor() + ";");
        }

        statutPieChart.setTitle("Répartition par statut");
        statutPieChart.setLabelsVisible(true);
        statutPieChart.setLegendVisible(true);
    }

    private void loadBarChart() {
        prioriteBarChart.getData().clear();

        var data = service.getBarChartData(tickets);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Nombre de tickets");

        for (var stat : data) {
            XYChart.Data<String, Number> bar = new XYChart.Data<>(stat.getLabel(), stat.getValue());
            series.getData().add(bar);

            // Ajouter couleur sur la barre
            bar.nodeProperty().addListener((obs, old, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle("-fx-bar-fill: " + stat.getColor() + ";");
                }
            });
        }

        prioriteBarChart.getData().add(series);
        prioriteBarChart.setTitle("Répartition par priorité");
        xAxis.setLabel("Priorité");
        yAxis.setLabel("Nombre de tickets");
    }

    private void loadLabels() {
        int total = tickets.size();
        var stats = service.countByStatut(tickets);
        double tauxResolution = service.getResolutionRate(tickets);
        double tempsMoyen = service.getAverageResolutionTime(tickets);

        lblTotalTickets.setText(String.valueOf(total));
        lblTauxResolution.setText(String.format("%.1f%%", tauxResolution));
        lblTempsMoyen.setText(String.format("%.1f h", tempsMoyen));
        lblTicketsOuverts.setText(String.valueOf(stats.getOrDefault("open", 0)));
        lblTicketsEnCours.setText(String.valueOf(stats.getOrDefault("in_progress", 0)));
        lblTicketsResolus.setText(String.valueOf(stats.getOrDefault("resolved", 0)));
        lblTicketsFermes.setText(String.valueOf(stats.getOrDefault("closed", 0)));
    }

    private void loadEmptyData() {
        statutPieChart.setData(FXCollections.observableArrayList());
        prioriteBarChart.getData().clear();

        lblTotalTickets.setText("0");
        lblTauxResolution.setText("0%");
        lblTempsMoyen.setText("0 h");
        lblTicketsOuverts.setText("0");
        lblTicketsEnCours.setText("0");
        lblTicketsResolus.setText("0");
        lblTicketsFermes.setText("0");
    }
}