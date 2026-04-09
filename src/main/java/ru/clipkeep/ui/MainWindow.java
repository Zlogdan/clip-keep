package ru.clipkeep.ui;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.clipkeep.service.ClipboardService;
import ru.clipkeep.service.HistoryService;

/**
 * Владеет основным JavaFX {@link Stage} и выступает мостом между
 * кодом инициализации приложения и {@link MainController}.
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
     * Создаёт и настраивает основное окно. Должен вызываться в потоке JavaFX
     * (например, внутри {@code Application.start()}).
     *
     * @param primaryStage объект Stage, предоставленный средой выполнения JavaFX.
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
                event.consume(); // скрываем окно вместо полного закрытия
                stage.hide();
            });
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load main window", e);
            throw new RuntimeException(e);
        }
    }

    /** Показывает окно и переносит его на передний план. */
    public void show() {
        if (stage != null) {
            stage.show();
            stage.toFront();
        }
    }

    /** Скрывает окно (приложение продолжает работать в трее). */
    public void hide() {
        if (stage != null) {
            stage.hide();
        }
    }

    /** Делегирует контроллеру перезагрузку списка. */
    public void refresh() {
        if (controller != null) {
            controller.refresh();
        }
    }
}
