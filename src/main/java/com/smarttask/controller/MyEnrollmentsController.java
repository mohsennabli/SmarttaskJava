package com.smarttask.controller;

import com.smarttask.model.InscriptionRow;
import com.smarttask.service.CertificateGenerationResult;
import com.smarttask.service.InscriptionService;
import com.smarttask.service.ProgressUpdateResult;
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
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class MyEnrollmentsController implements Initializable {

    private static final DateTimeFormatter DF = DateTimeFormatter.ISO_LOCAL_DATE;

    @FXML
    private TableView<InscriptionRow> enrollmentsTable;

    @FXML
    private TableColumn<InscriptionRow, String> colFormation;

    @FXML
    private TableColumn<InscriptionRow, String> colStart;

    @FXML
    private TableColumn<InscriptionRow, String> colEnd;

    @FXML
    private TableColumn<InscriptionRow, String> colStatut;

    @FXML
    private TableColumn<InscriptionRow, Integer> colProgress;

    @FXML
    private Spinner<Integer> progressSpinner;

    @FXML
    private Button updateProgressButton;

    @FXML
    private Button generateCertificateButton;

    @FXML
    private Button backButton;

    private final InscriptionService inscriptionService = new InscriptionService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colFormation.setCellValueFactory(new PropertyValueFactory<>("formationTitre"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colStart.setCellValueFactory(data -> {
            var d = data.getValue().getFormationDateDebut();
            return new ReadOnlyStringWrapper(d == null ? "" : d.format(DF));
        });
        colEnd.setCellValueFactory(data -> {
            var d = data.getValue().getFormationDateFin();
            return new ReadOnlyStringWrapper(d == null ? "" : d.format(DF));
        });
        colProgress.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getProgression()));
        colProgress.setCellFactory(column -> new TableCell<>() {
            private final ProgressBar bar = new ProgressBar();

            {
                bar.setMaxWidth(Double.MAX_VALUE);
                bar.getStyleClass().add("progress-bar-mid");
            }

            @Override
            protected void updateItem(Integer value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setGraphic(null);
                    return;
                }
                bar.setProgress(value / 100.0);
                setGraphic(bar);
            }
        });

        progressSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 0, 1));

        enrollmentsTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> onRowSelected(n));

        loadTable();
    }

    private void onRowSelected(InscriptionRow row) {
        if (row == null) {
            progressSpinner.getValueFactory().setValue(0);
            generateCertificateButton.setDisable(true);
            updateProgressButton.setDisable(true);
            return;
        }
        progressSpinner.getValueFactory().setValue(row.getProgression());
        updateProgressButton.setDisable(false);
        generateCertificateButton.setDisable(!inscriptionService.canGenerateCertificate(row));
    }

    private void loadTable() {
        List<InscriptionRow> rows = inscriptionService.listMyEnrollments();
        ObservableList<InscriptionRow> data = FXCollections.observableArrayList(rows);
        enrollmentsTable.setItems(data);
        if (!data.isEmpty()) {
            enrollmentsTable.getSelectionModel().selectFirst();
        } else {
            onRowSelected(null);
        }
    }

    @FXML
    private void handleUpdateProgress(ActionEvent event) {
        InscriptionRow row = enrollmentsTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            showAlert(Alert.AlertType.WARNING, "Selection", "Select an enrollment first.");
            return;
        }
        Integer value = progressSpinner.getValue();
        if (value == null) {
            showAlert(Alert.AlertType.WARNING, "Invalid", "Enter a progress value between 0 and 100.");
            return;
        }
        ProgressUpdateResult result = inscriptionService.updateProgress(row.getInscriptionId(), value);
        switch (result) {
            case SUCCESS -> {
                showAlert(Alert.AlertType.INFORMATION, "Saved", "Progress updated.");
                loadTable();
                selectByInscriptionId(row.getInscriptionId());
            }
            case NOT_LOGGED_IN -> showAlert(Alert.AlertType.ERROR, "Session", "You must be logged in.");
            case NOT_FOUND -> showAlert(Alert.AlertType.ERROR, "Not found", "Enrollment not found.");
            case FORBIDDEN -> showAlert(Alert.AlertType.ERROR, "Forbidden", "You cannot update this enrollment.");
            case DB_ERROR -> showAlert(Alert.AlertType.ERROR, "Error", "Could not save progress.");
        }
    }

    private void selectByInscriptionId(int id) {
        for (InscriptionRow r : enrollmentsTable.getItems()) {
            if (r.getInscriptionId() == id) {
                enrollmentsTable.getSelectionModel().select(r);
                return;
            }
        }
    }

    @FXML
    private void handleGenerateCertificate(ActionEvent event) {
        InscriptionRow row = enrollmentsTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            showAlert(Alert.AlertType.WARNING, "Selection", "Select an enrollment first.");
            return;
        }
        if (!inscriptionService.canGenerateCertificate(row)) {
            showAlert(Alert.AlertType.WARNING, "Progress", "Progress must be 100% to generate a certificate.");
            return;
        }
        var user = AppSession.getCurrentUser();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save certificate");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        String base = "Certificate_formation_" + row.getFormationId();
        if (user != null) {
            base += "_user_" + user.getIduser();
        }
        chooser.setInitialFileName(base + ".pdf");
        Stage stage = (Stage) generateCertificateButton.getScene().getWindow();
        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        Path path = file.toPath();
        CertificateGenerationResult result = inscriptionService.generateCertificateForInscription(
                row.getInscriptionId(), path);
        switch (result) {
            case SUCCESS -> showAlert(Alert.AlertType.INFORMATION, "Certificate",
                    "Certificate saved to:\n" + path.toAbsolutePath());
            case NOT_LOGGED_IN -> showAlert(Alert.AlertType.ERROR, "Session", "You must be logged in.");
            case NOT_FOUND -> showAlert(Alert.AlertType.ERROR, "Not found", "Enrollment not found.");
            case FORBIDDEN -> showAlert(Alert.AlertType.ERROR, "Forbidden", "You cannot generate this certificate.");
            case PROGRESS_INCOMPLETE -> showAlert(Alert.AlertType.WARNING, "Progress", "Complete the formation first.");
            case FORMATION_NOT_FOUND -> showAlert(Alert.AlertType.ERROR, "Error", "Formation not found.");
            case IO_ERROR -> showAlert(Alert.AlertType.ERROR, "IO Error", "Could not write the PDF file.");
            case DB_ERROR -> showAlert(Alert.AlertType.ERROR, "Error", "PDF written but database update failed.");
        }
        loadTable();
        selectByInscriptionId(row.getInscriptionId());
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smarttask/formations.fxml"));
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to return to formations.");
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
