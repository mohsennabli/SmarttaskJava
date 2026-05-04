module com.smarttask {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;
    requires java.net.http;
    requires java.desktop;
    requires jdk.httpserver;
    requires jdk.jsobject;
    requires jbcrypt;
    requires jakarta.mail;
    requires jakarta.activation;
    requires org.apache.pdfbox;
    requires kernel;
    requires io;
    requires layout;
    requires com.google.gson;

    opens com.smarttask to javafx.graphics, javafx.fxml;
    opens com.smarttask.controller to javafx.fxml;
    opens com.smarttask.service to com.google.gson;

    exports com.smarttask;
    exports com.smarttask.model;
    exports com.smarttask.dao;
    exports com.smarttask.util;
    exports com.smarttask.service;
}
