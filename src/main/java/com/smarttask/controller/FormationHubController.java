package com.smarttask.controller;

import com.smarttask.model.Formation;
import com.smarttask.model.User;
import com.smarttask.service.EnrollmentResult;
import com.smarttask.service.FormationCrudResult;
import com.smarttask.service.FormationService;
import com.smarttask.service.InscriptionService;
import com.smarttask.util.AppSession;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class FormationHubController implements Initializable {

    @FXML
    private TableView<Formation> formationsTable;

    @FXML
    private TableColumn<Formation, String> colTitre;

    @FXML
    private TableColumn<Formation, String> colNiveau;

    @FXML
    private TableColumn<Formation, String> colCategorie;

    @FXML
    private TableColumn<Formation, String> colDateDebut;

    @FXML
    private TableColumn<Formation, String> colDateFin;

    @FXML
    private TableColumn<Formation, String> colCapacity;

    @FXML
    private TableColumn<Formation, String> colStatut;

    @FXML
    private TextField searchField;

    @FXML
    private Button searchButton;

    @FXML
    private Button resetButton;

    @FXML
    private Button addFormationButton;

    @FXML
    private Button editFormationButton;

    @FXML
    private Button deleteFormationButton;

    @FXML
    private Button enrollButton;

    @FXML
    private Button myEnrollmentsButton;

    @FXML
    private Button backButton;

    @FXML
    private Label detailTitleLabel;

    @FXML
    private Label detailMetaLabel;

    @FXML
    private Label detailDescriptionLabel;

    private final FormationService formationService = new FormationService();
    private final InscriptionService inscriptionService = new InscriptionService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colNiveau.setCellValueFactory(new PropertyValueFactory<>("niveau"));
        colCategorie.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        colDateDebut.setCellValueFactory(data -> {
            Formation f = data.getValue();
            if (f.getDateDebut() == null) {
                return new ReadOnlyStringWrapper("");
            }
            return new ReadOnlyStringWrapper(f.getDateDebut().toString());
        });
        colDateFin.setCellValueFactory(data -> {
            Formation f = data.getValue();
            if (f.getDateFin() == null) {
                return new ReadOnlyStringWrapper("");
            }
            return new ReadOnlyStringWrapper(f.getDateFin().toString());
        });
        colCapacity.setCellValueFactory(data -> {
            Integer cap = data.getValue().getCapacity();
            if (cap == null) {
                return new ReadOnlyStringWrapper("Unlimited");
            }
            return new ReadOnlyStringWrapper(String.valueOf(cap));
        });

        formationsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            updateDetailPanel(newV);
            updateEnrollButtonState(newV);
        });

        boolean manager = formationService.isCurrentUserManager();
        addFormationButton.setVisible(manager);
        addFormationButton.setManaged(manager);
        editFormationButton.setVisible(manager);
        editFormationButton.setManaged(manager);
        deleteFormationButton.setVisible(manager);
        deleteFormationButton.setManaged(manager);

        loadFormations();
    }

    private void loadFormations() {
        List<Formation> list = formationService.listAll();
        ObservableList<Formation> data = FXCollections.observableArrayList(list);
        formationsTable.setItems(data);
        if (!data.isEmpty()) {
            formationsTable.getSelectionModel().selectFirst();
        } else {
            clearDetailPanel();
        }
    }

    private void clearDetailPanel() {
        detailTitleLabel.setText("—");
        detailMetaLabel.setText("");
        detailDescriptionLabel.setText("No formations available.");
        enrollButton.setDisable(true);
    }

    private void updateDetailPanel(Formation f) {
        if (f == null) {
            detailTitleLabel.setText("—");
            detailMetaLabel.setText("");
            detailDescriptionLabel.setText("Select a formation.");
            return;
        }
        detailTitleLabel.setText(f.getTitre() != null ? f.getTitre() : "");
        String meta = "Level: " + nullSafe(f.getNiveau())
                + "  |  Category: " + nullSafe(f.getCategorie())
                + "  |  Duration: " + (f.getDuree() != null ? f.getDuree() + " h" : "—")
                + "  |  Status: " + nullSafe(f.getStatut());
        detailMetaLabel.setText(meta);
        detailDescriptionLabel.setText(f.getDescription() != null && !f.getDescription().isBlank()
                ? f.getDescription()
                : "No description.");
    }

    private static String nullSafe(String s) {
        return s == null ? "—" : s;
    }

    private void updateEnrollButtonState(Formation f) {
        if (f == null) {
            enrollButton.setDisable(true);
            return;
        }
        User user = AppSession.getCurrentUser();
        if (user == null) {
            enrollButton.setDisable(true);
            return;
        }
        boolean enrolled = inscriptionService.isEnrolledIn(user.getIduser(), f.getId());
        enrollButton.setDisable(enrolled);
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim();
        List<Formation> list = formationService.search(keyword);
        formationsTable.setItems(FXCollections.observableArrayList(list));
        if (!list.isEmpty()) {
            formationsTable.getSelectionModel().selectFirst();
        } else {
            clearDetailPanel();
        }
    }

    @FXML
    private void handleReset(ActionEvent event) {
        searchField.clear();
        loadFormations();
    }

    @FXML
    private void handleAddFormation(ActionEvent event) {
        if (!formationService.isCurrentUserManager()) {
            showAlert(Alert.AlertType.ERROR, "Forbidden", "Only managers can add formations.");
            return;
        }
        openFormationEditor(null);
    }

    @FXML
    private void handleEditFormation(ActionEvent event) {
        if (!formationService.isCurrentUserManager()) {
            showAlert(Alert.AlertType.ERROR, "Forbidden", "Only managers can edit formations.");
            return;
        }
        Formation selected = formationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select a formation to edit.");
            return;
        }
        openFormationEditor(selected);
    }

    @FXML
    private void handleDeleteFormation(ActionEvent event) {
        if (!formationService.isCurrentUserManager()) {
            showAlert(Alert.AlertType.ERROR, "Forbidden", "Only managers can delete formations.");
            return;
        }
        Formation selected = formationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select a formation to delete.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm delete");
        confirmation.setHeaderText(null);
        confirmation.setContentText("Delete \"" + selected.getTitre() + "\"? All enrollments for this formation will be removed.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        FormationCrudResult deleteResult = formationService.deleteFormation(selected.getId());
        switch (deleteResult) {
            case SUCCESS -> {
                showAlert(Alert.AlertType.INFORMATION, "Deleted", "Formation deleted.");
                loadFormations();
            }
            case FORBIDDEN -> showAlert(Alert.AlertType.ERROR, "Forbidden", "Only managers can delete formations.");
            case NOT_FOUND -> showAlert(Alert.AlertType.WARNING, "Not found", "Formation was already removed.");
            case VALIDATION_ERROR, DB_ERROR -> showAlert(Alert.AlertType.ERROR, "Error", "Could not delete formation.");
        }
    }

    private void openFormationEditor(Formation existing) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smarttask/edit-formation.fxml"));
            Stage editStage = new Stage();
            editStage.initModality(Modality.WINDOW_MODAL);
            editStage.initOwner(formationsTable.getScene().getWindow());
            editStage.setScene(new Scene(loader.load()));

            EditFormationController controller = loader.getController();
            if (existing == null) {
                editStage.setTitle("Add formation");
                controller.prepareCreate();
            } else {
                editStage.setTitle("Edit formation");
                controller.prepareEdit(existing);
            }

            editStage.setMinWidth(640);
            editStage.setMinHeight(520);
            editStage.showAndWait();
            loadFormations();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to open formation editor.");
        }
    }

    @FXML
    private void handleEnroll(ActionEvent event) {
        Formation selected = formationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select a formation to enroll.");
            return;
        }
        EnrollmentResult result = inscriptionService.enrollCurrentUser(selected.getId());
        switch (result) {
            case SUCCESS -> {
                showAlert(Alert.AlertType.INFORMATION, "Enrolled", "You are now enrolled in this formation.");
                updateEnrollButtonState(selected);
            }
            case NOT_LOGGED_IN -> showAlert(Alert.AlertType.ERROR, "Session", "You must be logged in to enroll.");
            case FORMATION_NOT_FOUND -> showAlert(Alert.AlertType.ERROR, "Not found", "Formation no longer exists.");
            case ALREADY_ENROLLED -> showAlert(Alert.AlertType.WARNING, "Already enrolled",
                    "You are already enrolled in this formation.");
            case FORMATION_FULL -> showAlert(Alert.AlertType.WARNING, "Full",
                    "This formation has reached its capacity.");
            case DB_ERROR -> showAlert(Alert.AlertType.ERROR, "Error", "Could not save enrollment. Please try again.");
        }
    }

    @FXML
    private void handleMyEnrollments(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smarttask/my-enrollments.fxml"));
            Stage stage = (Stage) myEnrollmentsButton.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to open My Enrollments.");
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smarttask/users.fxml"));
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to return to user list.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
