package org.esprit.gestionprojet.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.esprit.gestionprojet.AppContext;
import org.esprit.gestionprojet.AppRouter;
import org.esprit.gestionprojet.model.Projet;
import org.esprit.gestionprojet.model.Tache;
import org.esprit.gestionprojet.util.AlertUtil;

import java.time.LocalDate;

public class TacheFormController {
    @FXML
    private Label pageTitle;
    @FXML
    private TextField libelleField;
    @FXML
    private ComboBox<String> prioriteCombo;
    @FXML
    private DatePicker dateLimitePicker;
    @FXML
    private ComboBox<String> etatCombo;
    @FXML
    private ComboBox<Projet> projetCombo;
    @FXML
    private Button dashboardButton;
    @FXML
    private VBox sidebarPanel;
    @FXML
    private HBox frontTopBar;
    @FXML
    private Label libelleError;
    @FXML
    private Label prioriteError;
    @FXML
    private Label dateLimiteError;
    @FXML
    private Label etatError;
    @FXML
    private Label projetError;

    private Tache tacheToEdit;
    private Integer selectedProjetId;

    @FXML
    private void initialize() {
        prioriteCombo.setItems(FXCollections.observableArrayList("basse", "moyenne", "haute"));
        etatCombo.setItems(FXCollections.observableArrayList("a_faire", "en_cours", "termine"));
        projetCombo.setItems(FXCollections.observableArrayList(AppContext.projetRepository().findAll()));
        updateDashboardButtonVisibility();

        boolean admin = "BACKOFFICE".equalsIgnoreCase(AppContext.sessionService().getOffice());
        if (sidebarPanel != null) {
            sidebarPanel.setVisible(admin);
            sidebarPanel.setManaged(admin);
        }
        if (frontTopBar != null) {
            frontTopBar.setVisible(!admin);
            frontTopBar.setManaged(!admin);
        }

        prioriteCombo.getSelectionModel().select("moyenne");
        etatCombo.getSelectionModel().select("a_faire");

        projetCombo.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Projet item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNom());
            }
        });
        projetCombo.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Projet item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNom());
            }
        });
    }

    public void setTacheToEdit(Tache tache) {
        this.tacheToEdit = tache;
        if (tache == null) {
            pageTitle.setText("Creer une tache");
            applySelectedProject();
            return;
        }

        pageTitle.setText("Modifier la tache");
        libelleField.setText(tache.getLibelle());
        prioriteCombo.getSelectionModel().select(tache.getPriorite());
        dateLimitePicker.setValue(tache.getDateLimite());
        etatCombo.getSelectionModel().select(tache.getEtat());

        projetCombo.getSelectionModel().select(
            AppContext.projetRepository().findAll().stream()
                        .filter(p -> p.getId() == tache.getProjetId())
                        .findFirst()
                        .orElse(null));
    }

    public void setSelectedProjetId(Integer selectedProjetId) {
        this.selectedProjetId = selectedProjetId;
        applySelectedProject();
    }

    @FXML
    private void saveTache() {
        clearErrors();

        String libelle = safeValue(libelleField.getText());
        String priorite = prioriteCombo.getValue();
        LocalDate dateLimite = dateLimitePicker.getValue();
        String etat = etatCombo.getValue();
        Projet projet = projetCombo.getValue();

        boolean valid = true;

        if (libelle.isEmpty()) {
            libelleError.setText("Le libelle est obligatoire.");
            valid = false;
        }
        if (priorite == null || priorite.isEmpty()) {
            prioriteError.setText("La priorite est obligatoire.");
            valid = false;
        }
        if (dateLimite == null) {
            dateLimiteError.setText("La date limite est obligatoire.");
            valid = false;
        } else if (!dateLimite.isAfter(LocalDate.now())) {
            dateLimiteError.setText("La date limite doit etre superieure a aujourd'hui.");
            valid = false;
        }
        if (etat == null || etat.isEmpty()) {
            etatError.setText("L'etat est obligatoire.");
            valid = false;
        }
        if (projet == null) {
            projetError.setText("Le projet est obligatoire.");
            valid = false;
        }

        if (!valid) {
            return;
        }

        if (tacheToEdit == null) {
            Tache created = new Tache(0, libelle, priorite, dateLimite, etat, projet.getId());
            int createdId = AppContext.tacheRepository().insert(created);
            created.setId(createdId);
            AlertUtil.info("Tache", "Tache creee avec succes.");
        } else {
            tacheToEdit.setLibelle(libelle);
            tacheToEdit.setPriorite(priorite);
            tacheToEdit.setDateLimite(dateLimite);
            tacheToEdit.setEtat(etat);
            tacheToEdit.setProjetId(projet.getId());
            AppContext.tacheRepository().update(tacheToEdit);
            AlertUtil.info("Tache", "Tache modifiee avec succes.");
        }

        AppRouter.showTacheList(projet.getId());
    }

    @FXML
    private void cancel() {
        if (selectedProjetId != null) {
            AppRouter.showTacheList(selectedProjetId);
        } else if (projetCombo.getValue() != null) {
            AppRouter.showTacheList(projetCombo.getValue().getId());
        } else {
            AppRouter.showTacheList();
        }
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
    private void goProjects() {
        AppRouter.showProjetList();
    }

    @FXML
    private void goTaches() {
        AppRouter.showTacheList(selectedProjetId);
    }

    @FXML
    private void logout() {
        AppContext.sessionService().logout();
        AppRouter.showLanding();
    }

    private String safeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private void applySelectedProject() {
        if (projetCombo == null || selectedProjetId == null) {
            return;
        }

        Projet selected = AppContext.projetRepository().findAll().stream()
                .filter(p -> p.getId() == selectedProjetId)
                .findFirst()
                .orElse(null);

        if (selected != null) {
            projetCombo.getSelectionModel().select(selected);
        }
    }

    private void clearErrors() {
        libelleError.setText("");
        prioriteError.setText("");
        dateLimiteError.setText("");
        etatError.setText("");
        projetError.setText("");
    }

    private void updateDashboardButtonVisibility() {
        if (dashboardButton == null) {
            return;
        }

        boolean admin = "BACKOFFICE".equalsIgnoreCase(AppContext.sessionService().getOffice());
        dashboardButton.setVisible(admin);
        dashboardButton.setManaged(admin);
    }
}
