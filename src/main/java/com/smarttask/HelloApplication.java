package com.smarttask;

import javafx.application.Application;
import javafx.stage.Stage;
import com.smarttask.util.AlertUtil;
import com.smarttask.util.DBconnexion;

public class HelloApplication extends Application {
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