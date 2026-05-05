package com.smarttask.controller;

import com.smarttask.dao.ProjetDAO;
import com.smarttask.dao.TacheDAO;
import com.smarttask.model.Projet;
import com.smarttask.model.Tache;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class TacheFormController implements Initializable {
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
    private Label libelleError;
    @FXML
    private Label prioriteError;
    @FXML
    private Label dateLimiteError;
    @FXML
    private Label etatError;
    @FXML
    private Label projetError;

    @FXML
    private Button saveBtn;
    @FXML
    private Button cancelBtn;

    private final TacheDAO tacheDAO = new TacheDAO();
    private final ProjetDAO projetDAO = new ProjetDAO();
    private Tache tacheToEdit;
    private Integer selectedProjetId;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        prioriteCombo.getItems().addAll("basse", "moyenne", "haute");
        etatCombo.getItems().addAll("a_faire", "en_cours", "termine");

        // Load projects for ComboBox
        projetCombo.getItems().addAll(projetDAO.getAllProjets());
        projetCombo.setVisibleRowCount(5);
    }

    public void setTacheToEdit(Tache tache) {
        this.tacheToEdit = tache;

        if (tache == null) {
            pageTitle.setText("Créer une tâche");
        } else {
            pageTitle.setText("Modifier la tâche");
            libelleField.setText(tache.getLibelle());
            prioriteCombo.setValue(tache.getPriorite());
            dateLimitePicker.setValue(tache.getDateLimite());
            etatCombo.setValue(tache.getEtat());
            
            var projet = projetDAO.getProjetById(tache.getProjetId());
            if (projet.isPresent()) {
                projetCombo.setValue(projet.get());
            }
        }
    }

    public void setSelectedProjetId(Integer projetId) {
        this.selectedProjetId = projetId;
        if (projetId != null) {
            var projet = projetDAO.getProjetById(projetId);
            if (projet.isPresent()) {
                projetCombo.setValue(projet.get());
            }
        }
    }

    @FXML
    private void saveTache() {
        // Clear errors
        libelleError.setText("");
        prioriteError.setText("");
        dateLimiteError.setText("");
        etatError.setText("");
        projetError.setText("");

        // Validate
        String libelle = libelleField.getText().trim();
        String priorite = prioriteCombo.getValue();
        LocalDate dateLimite = dateLimitePicker.getValue();
        String etat = etatCombo.getValue();
        Projet projet = projetCombo.getValue();

        boolean isValid = true;

        if (libelle.isEmpty()) {
            libelleError.setText("Le libellé est obligatoire");
            isValid = false;
        }

        if (priorite == null) {
            prioriteError.setText("La priorité est obligatoire");
            isValid = false;
        }

        if (dateLimite == null || dateLimite.isBefore(LocalDate.now())) {
            dateLimiteError.setText("La date limite doit être après aujourd'hui");
            isValid = false;
        }

        if (etat == null) {
            etatError.setText("L'état est obligatoire");
            isValid = false;
        }

        if (projet == null) {
            projetError.setText("Le projet est obligatoire");
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        Tache tache = new Tache(libelle, priorite, dateLimite, etat, projet.getId());

        boolean success;
        if (tacheToEdit == null) {
            // Create new
            int id = tacheDAO.insertTache(tache);
            success = id > 0;
        } else {
            // Edit existing
            tache.setId(tacheToEdit.getId());
            success = tacheDAO.updateTache(tache);
        }

        if (success) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setContentText("La tâche a été enregistrée avec succès");
            alert.showAndWait();
            closeWindow();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setContentText("Erreur lors de l'enregistrement de la tâche");
            alert.showAndWait();
        }
    }

    @FXML
    private void cancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) libelleField.getScene().getWindow();
        stage.close();
    }
}
