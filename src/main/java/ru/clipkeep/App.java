package ru.clipkeep;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import ru.clipkeep.config.AppConfig;
import ru.clipkeep.service.ClipboardService;
import ru.clipkeep.service.ClipboardWatcher;
import ru.clipkeep.service.HistoryService;
import ru.clipkeep.service.StorageService;
import ru.clipkeep.tray.TrayService;
import ru.clipkeep.ui.MainWindow;

import javax.swing.*;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application entry point.
 * <p>
 * Start-up sequence:
 * <ol>
 *   <li>Load {@code config.json} (fall back to defaults if absent).</li>
 *   <li>Initialise all service objects.</li>
 *   <li>Build the JavaFX primary stage (hidden by default).</li>
 *   <li>Install the system-tray icon (on the AWT EDT).</li>
 *   <li>Start the clipboard-watcher thread.</li>
 * </ol>
 * The application stays alive until the user chooses "Выход" from the tray menu.
 */
public class App extends Application {

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
    private static final String CONFIG_FILE = "config.json";

    // -----------------------------------------------------------------------
    // Service singletons – created once and shared across the app
    // -----------------------------------------------------------------------
    private AppConfig config;
    private ClipboardService clipboardService;
    private HistoryService historyService;
    private ClipboardWatcher clipboardWatcher;
    private MainWindow mainWindow;
    private TrayService trayService;

    // -----------------------------------------------------------------------
    // JavaFX Application lifecycle
    // -----------------------------------------------------------------------

    @Override
    public void init() {
        config = loadConfig();

        clipboardService = new ClipboardService();
        StorageService storageService = new StorageService(config);
        historyService = new HistoryService(storageService, config);

        mainWindow = new MainWindow(historyService, clipboardService);

        clipboardWatcher = new ClipboardWatcher(
                clipboardService,
                config,
                text -> {
                    historyService.add(text);
                    Platform.runLater(mainWindow::refresh);
                });
    }

    @Override
    public void start(Stage primaryStage) {
        // Keep the JVM alive even when the window is hidden
        Platform.setImplicitExit(false);

        // Build the main window (hidden initially)
        mainWindow.build(primaryStage);

        // Install the tray icon on the AWT Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            trayService = new TrayService(historyService, mainWindow);
            trayService.install();
        });

        // Start polling the clipboard
        clipboardWatcher.start();

        // Show the window immediately on first launch
        mainWindow.show();

        LOGGER.info("ClipKeep started");
    }

    @Override
    public void stop() {
        if (clipboardWatcher != null) {
            clipboardWatcher.stop();
        }
        LOGGER.info("ClipKeep stopped");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private AppConfig loadConfig() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try {
                return new ObjectMapper().readValue(configFile, AppConfig.class);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to read config.json, using defaults", e);
            }
        } else {
            LOGGER.info("config.json not found, using default configuration");
        }
        return new AppConfig();
    }

    // -----------------------------------------------------------------------
    // Entry point
    // -----------------------------------------------------------------------

    public static void main(String[] args) {
        // Enable AWT system tray support alongside JavaFX
        System.setProperty("java.awt.headless", "false");
        launch(args);
    }
}
