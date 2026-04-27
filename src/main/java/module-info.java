module com.smarttask {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;
    requires java.net.http;
    requires jdk.httpserver;
    requires jbcrypt;
    requires kernel;
    requires io;
    requires layout;

    opens com.smarttask to javafx.graphics, javafx.fxml;
    opens com.smarttask.controller to javafx.fxml;

    exports com.smarttask;
    exports com.smarttask.model;
    exports com.smarttask.dao;
    exports com.smarttask.util;
    exports com.smarttask.service;
}
