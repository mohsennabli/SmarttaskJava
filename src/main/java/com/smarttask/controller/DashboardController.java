package com.smarttask.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import com.smarttask.AppContext;
import com.smarttask.AppRouter;
import com.smarttask.model.Projet;
import com.smarttask.model.Tache;
import com.smarttask.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class DashboardController {
    @FXML
    private Label welcomeLabel;
    @FXML
    private Label officeLabel;
    @FXML
    private Label totalProjectsValue;
    @FXML
    private Label activeProjectsValue;
    @FXML
    private Label completedProjectsValue;
    @FXML
    private Label overdueTasksValue;
    @FXML
    private Button openTachesButton;
    @FXML
    private Button openProjetsButton;
    @FXML
    private PieChart projectStatusChart;
    @FXML
    private BarChart<String, Number> tasksByProjectChart;
    @FXML
    private CategoryAxis tasksByProjectAxis;
    @FXML
    private NumberAxis tasksCountAxis;

    @FXML
    private void initialize() {
        if (projectStatusChart != null) {
            projectStatusChart.setLabelsVisible(true);
            projectStatusChart.setLegendVisible(true);
        }
        if (tasksByProjectChart != null) {
            tasksByProjectChart.setLegendVisible(false);
        }
        if (tasksByProjectAxis != null) {
            tasksByProjectAxis.setTickLabelRotation(25);
        }
        if (tasksCountAxis != null) {
            tasksCountAxis.setForceZeroInRange(true);
            tasksCountAxis.setMinorTickVisible(false);
        }
    }

    public void loadSessionData() {
        User currentUser = AppContext.sessionService().getCurrentUser();
        String office = AppContext.sessionService().getOffice();

        if (currentUser == null || office == null) {
            AppRouter.showLanding();
            return;
        }

        welcomeLabel.setText("Welcome, " + currentUser.getFullName());
        officeLabel.setText("Connected to " + office);

        boolean admin = "BACKOFFICE".equalsIgnoreCase(office);
        openProjetsButton.setVisible(admin);
        openProjetsButton.setManaged(admin);
        openTachesButton.setVisible(admin);
        openTachesButton.setManaged(admin);

        if (!admin) {
            AppRouter.showFrontHome();
            return;
        }

        refreshStatistics();
    }

    private void refreshStatistics() {
        List<Projet> projets = AppContext.projetRepository().findAll();
        List<Tache> taches = AppContext.tacheRepository().findAll();

        totalProjectsValue.setText(String.valueOf(projets.size()));
        activeProjectsValue.setText(String.valueOf(countProjectsByStatus(projets, "actif")));
        completedProjectsValue.setText(String.valueOf(countProjectsByStatus(projets, "termine")));
        overdueTasksValue.setText(String.valueOf(countOverdueTasks(taches)));

        projectStatusChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Actifs", countProjectsByStatus(projets, "actif")),
                new PieChart.Data("Termines", countProjectsByStatus(projets, "termine")),
                new PieChart.Data("En attente", countProjectsByStatus(projets, "en_attente"))
        ));

        XYChart.Series<String, Number> tasksSeries = new XYChart.Series<>();
        buildTaskSeries(tasksSeries, projets, taches);
        tasksByProjectChart.getData().setAll(tasksSeries);
    }

    private void buildTaskSeries(XYChart.Series<String, Number> series, List<Projet> projets, List<Tache> taches) {
        Map<Integer, String> projectNames = projets.stream()
                .collect(Collectors.toMap(Projet::getId, Projet::getNom));

        taches.stream()
                .collect(Collectors.groupingBy(tache -> projectNames.getOrDefault(tache.getProjetId(), "Projet supprime"), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .forEach(entry -> series.getData().add(new XYChart.Data<>(shorten(entry.getKey()), entry.getValue())));
    }

    private String shorten(String value) {
        if (value == null) {
            return "-";
        }
        return value.length() > 22 ? value.substring(0, 19) + "..." : value;
    }

    private long countProjectsByStatus(List<Projet> projets, String status) {
        return projets.stream()
                .filter(projet -> normalize(projet.getStatut()).equals(status))
                .count();
    }

    private long countOverdueTasks(List<Tache> taches) {
        LocalDate today = LocalDate.now();
        return taches.stream()
                .filter(tache -> tache.getDateLimite() != null)
                .filter(tache -> tache.getDateLimite().isBefore(today))
                .filter(tache -> !"termine".equals(normalize(tache.getEtat())))
                .count();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    @FXML
    private void openProjets() {
        AppRouter.showProjetList();
    }

    @FXML
    private void openTaches() {
        AppRouter.showTacheList();
    }

    @FXML
    private void goDashboard() {
        AppRouter.showDashboard();
    }

    @FXML
    private void goHome() {
        AppRouter.showFrontHome();
    }

    @FXML
    private void logout() {
        AppContext.sessionService().logout();
        AppRouter.showLanding();
    }
}
