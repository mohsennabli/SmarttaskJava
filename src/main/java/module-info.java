module org.esprit.gestionprojet {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.net.http;
    requires java.desktop;
    requires jbcrypt;
    requires jakarta.mail;
    requires org.apache.pdfbox;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.bootstrapfx.core;

    opens org.esprit.gestionprojet to javafx.fxml;
    opens org.esprit.gestionprojet.controller to javafx.fxml;

    exports org.esprit.gestionprojet;
    exports org.esprit.gestionprojet.service;
    exports org.esprit.gestionprojet.model;
}