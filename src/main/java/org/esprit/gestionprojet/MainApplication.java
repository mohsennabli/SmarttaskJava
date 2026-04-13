package org.esprit.gestionprojet;

import javafx.application.Application;
import javafx.stage.Stage;
import org.esprit.gestionprojet.util.AlertUtil;
import org.esprit.gestionprojet.util.DBconnexion;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) {
        try {
            DBconnexion.getInstance().getCnx();
        } catch (Exception e) {
            AlertUtil.error("Database", "Database initialization failed: " + e.getMessage());
            throw e;
        }

        AppRouter.init(stage);
        AppRouter.showLanding();
    }

    public static void main(String[] args) {
        launch();
    }
}
