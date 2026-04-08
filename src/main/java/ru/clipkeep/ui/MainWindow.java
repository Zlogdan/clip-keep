package ru.clipkeep.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.clipkeep.service.ClipboardService;
import ru.clipkeep.service.HistoryService;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owns the primary JavaFX {@link Stage} and acts as the bridge between the
 * application bootstrap code and the {@link MainController}.
 */
public class MainWindow {

    private static final Logger LOGGER = Logger.getLogger(MainWindow.class.getName());

    private final HistoryService historyService;
    private final ClipboardService clipboardService;

    private Stage stage;
    private MainController controller;

    public MainWindow(HistoryService historyService, ClipboardService clipboardService) {
        this.historyService = historyService;
        this.clipboardService = clipboardService;
    }

    /**
     * Builds and shows the primary window.  Must be called on the JavaFX
     * Application Thread (e.g. inside {@code Application.start()}).
     *
     * @param primaryStage the stage provided by the JavaFX runtime.
     */
    public void build(Stage primaryStage) {
        this.stage = primaryStage;

        try {
            URL fxmlUrl = getClass().getResource("/fxml/main.fxml");
            if (fxmlUrl == null) {
                throw new IOException("Cannot find /fxml/main.fxml on the classpath");
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            controller = loader.getController();
            controller.init(historyService, clipboardService);
            controller.refresh();

            Scene scene = new Scene(root, 700, 500);
            URL cssUrl = getClass().getResource("/fxml/style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            stage.setTitle("ClipKeep");
            stage.setScene(scene);
            stage.setOnCloseRequest(event -> {
                event.consume(); // hide instead of closing
                stage.hide();
            });
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load main window", e);
            throw new RuntimeException(e);
        }
    }

    /** Shows the window and brings it to the front. */
    public void show() {
        if (stage != null) {
            stage.show();
            stage.toFront();
        }
    }

    /** Hides the window (keeps the application running in the tray). */
    public void hide() {
        if (stage != null) {
            stage.hide();
        }
    }

    /** Delegates to the controller to reload the list. */
    public void refresh() {
        if (controller != null) {
            controller.refresh();
        }
    }
}
