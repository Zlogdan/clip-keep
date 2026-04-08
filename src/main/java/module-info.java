module ru.clipkeep {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires java.desktop;
    requires java.logging;

    opens ru.clipkeep to javafx.fxml;
    opens ru.clipkeep.ui to javafx.fxml;
    opens ru.clipkeep.config to com.fasterxml.jackson.databind;
    opens ru.clipkeep.model to com.fasterxml.jackson.databind;
    opens ru.clipkeep.service to javafx.fxml;

    exports ru.clipkeep;
    exports ru.clipkeep.config;
    exports ru.clipkeep.model;
    exports ru.clipkeep.service;
    exports ru.clipkeep.tray;
    exports ru.clipkeep.ui;
}
