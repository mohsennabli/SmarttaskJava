package org.esprit.gestionprojet;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.esprit.gestionprojet.controller.DashboardController;
import org.esprit.gestionprojet.controller.ProjetFormController;
import org.esprit.gestionprojet.controller.TacheFormController;
import org.esprit.gestionprojet.controller.FrontHomeController;
import org.esprit.gestionprojet.model.Projet;
import org.esprit.gestionprojet.model.Tache;

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
        showSimple("/org/esprit/gestionprojet/fxml/landing.fxml", "SmartTask JavaFX");
    }

    public static void showFrontHome() {
        showSimple("/org/esprit/gestionprojet/fxml/front-home.fxml", "Accueil");
    }

    public static void showBackofficeLogin() {
        showSimple("/org/esprit/gestionprojet/fxml/backoffice-login.fxml", "Backoffice Login");
    }

    public static void showFrontofficeLogin() {
        showSimple("/org/esprit/gestionprojet/fxml/frontoffice-login.fxml", "Frontoffice Login");
    }

    public static void showDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    AppRouter.class.getResource("/org/esprit/gestionprojet/fxml/dashboard.fxml"));
            Parent root = loader.load();
            DashboardController controller = loader.getController();
            controller.loadSessionData();
            applyScene(root, "Dashboard");
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load dashboard view", e);
        }
    }

    public static void showProjetList() {
        showSimple("/org/esprit/gestionprojet/fxml/projet-list.fxml", "Projets");
    }

    public static void showProjetForm(Projet projet) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    AppRouter.class.getResource("/org/esprit/gestionprojet/fxml/projet-form.fxml"));
            Parent root = loader.load();
            ProjetFormController controller = loader.getController();
            controller.setProjetToEdit(projet);
            applyScene(root, projet == null ? "Nouveau Projet" : "Modifier Projet");
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load projet form view", e);
        }
    }

    public static void showTacheList() {
        showSimple("/org/esprit/gestionprojet/fxml/tache-list.fxml", "Taches");
    }

    public static void showTacheList(Integer projetId) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    AppRouter.class.getResource("/org/esprit/gestionprojet/fxml/tache-list.fxml"));
            Parent root = loader.load();
            org.esprit.gestionprojet.controller.TacheListController controller = loader.getController();
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
                    AppRouter.class.getResource("/org/esprit/gestionprojet/fxml/tache-form.fxml"));
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
        scene.getStylesheets()
                .add(AppRouter.class.getResource("/org/esprit/gestionprojet/css/app.css").toExternalForm());
        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
    }
}
