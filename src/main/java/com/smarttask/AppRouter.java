package com.smarttask;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.smarttask.controller.DashboardController;
import com.smarttask.controller.ProjetFormController;
import com.smarttask.controller.TacheFormController;
import com.smarttask.controller.TacheListController;
import com.smarttask.model.Projet;
import com.smarttask.model.Tache;

import java.io.IOException;

public final class AppRouter {
    private static Stage stage;

    private AppRouter() {
    }

    public static void init(Stage primaryStage) {
        stage = primaryStage;
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
    }

    public static void showLanding() {
        // Use the application's own login view as the landing page
        showSimple("/com/smarttask/login.fxml", "SmartTask JavaFX");
    }

    public static void showFrontHome() {
        showSimple("/gestionprojet/fxml/front-home.fxml", "Accueil");
    }

    public static void showBackofficeLogin() {
        showSimple("/gestionprojet/fxml/backoffice-login.fxml", "Backoffice Login");
    }

    public static void showFrontofficeLogin() {
        showSimple("/gestionprojet/fxml/frontoffice-login.fxml", "Frontoffice Login");
    }

    public static void showDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    AppRouter.class.getResource("/gestionprojet/fxml/dashboard.fxml"));
            Parent root = loader.load();
            DashboardController controller = loader.getController();
            controller.loadSessionData();
            applyScene(root, "Dashboard");
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load dashboard view", e);
        }
    }

    public static void showProjetList() {
        showSimple("/gestionprojet/fxml/projet-list.fxml", "Projets");
    }

    public static void showProjetForm(Projet projet) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    AppRouter.class.getResource("/gestionprojet/fxml/projet-form.fxml"));
            Parent root = loader.load();
            ProjetFormController controller = loader.getController();
            controller.setProjetToEdit(projet);
            applyScene(root, projet == null ? "Nouveau Projet" : "Modifier Projet");
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load projet form view", e);
        }
    }

    public static void showTacheList() {
        showSimple("/gestionprojet/fxml/tache-list.fxml", "Taches");
    }

    public static void showTacheList(Integer projetId) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    AppRouter.class.getResource("/gestionprojet/fxml/tache-list.fxml"));
            Parent root = loader.load();
            TacheListController controller = loader.getController();
            controller.setProjetFilter(projetId);
            applyScene(root, projetId == null ? "Taches" : "Taches du projet");
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load tache list view", e);
        }
    }

    public static void showTacheForm(Tache tache) {
        showTacheForm(tache, null);
    }

    public static void showTacheForm(Tache tache, Integer projetId) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    AppRouter.class.getResource("/gestionprojet/fxml/tache-form.fxml"));
            Parent root = loader.load();
            TacheFormController controller = loader.getController();
            controller.setTacheToEdit(tache);
            controller.setSelectedProjetId(projetId);
            applyScene(root, tache == null ? "Nouvelle Tache" : "Modifier Tache");
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load tache form view", e);
        }
    }

    private static void showSimple(String resource, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(AppRouter.class.getResource(resource));
            Parent root = loader.load();
            applyScene(root, title);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load view: " + resource, e);
        }
    }

    private static void applyScene(Parent root, String title) {
        Scene scene = new Scene(root);
        // Use the project's stylesheet location
        var cssResource = AppRouter.class.getResource("/com/smarttask/styles/styles.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        }
        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
    }
}
