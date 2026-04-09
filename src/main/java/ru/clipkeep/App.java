package ru.clipkeep;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

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

/**
 * Точка входа приложения.
 * <p>
 * Последовательность запуска:
 * <ol>
 *   <li>Загрузка {@code config.json} (при отсутствии используются значения по умолчанию).</li>
 *   <li>Инициализация всех сервисов.</li>
 *   <li>Построение основного окна JavaFX (по умолчанию скрыто).</li>
 *   <li>Установка иконки в системный трей (в AWT EDT).</li>
 *   <li>Запуск потока наблюдения за буфером обмена.</li>
 * </ol>
 * Приложение работает, пока пользователь не выберет «Выход» в меню трея.
 */
public class App extends Application {

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
    private static final String CONFIG_FILE = "config.json";

    // -----------------------------------------------------------------------
    // Синглтоны сервисов — создаются один раз и используются во всём приложении
    // -----------------------------------------------------------------------
    private AppConfig config;
    private ClipboardService clipboardService;
    private HistoryService historyService;
    private ClipboardWatcher clipboardWatcher;
    private MainWindow mainWindow;
    private TrayService trayService;

    // -----------------------------------------------------------------------
    // Жизненный цикл JavaFX-приложения
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
        // Не завершать JVM, даже если окно скрыто
        Platform.setImplicitExit(false);

        // Построение главного окна (изначально скрыто)
        mainWindow.build(primaryStage);

        // Установка иконки трея в потоке диспетчеризации событий AWT
        SwingUtilities.invokeLater(() -> {
            trayService = new TrayService(historyService, mainWindow);
            trayService.install();
        });

        // Запуск опроса буфера обмена
        clipboardWatcher.start();

        // Показ окна сразу при первом запуске
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
    // Вспомогательные методы
    // -----------------------------------------------------------------------

    private AppConfig loadConfig() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try {
                return new ObjectMapper().readValue(configFile, AppConfig.class);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Не удалось прочитать config.json, используются значения по умолчанию", e);
            }
        } else {
            LOGGER.info("Файл config.json не найден, используется конфигурация по умолчанию");
        }
        return new AppConfig();
    }

    // -----------------------------------------------------------------------
    // Точка входа
    // -----------------------------------------------------------------------

    public static void main(String[] args) {
        // Включить поддержку системного трея AWT вместе с JavaFX
        System.setProperty("java.awt.headless", "false");
        launch(args);
    }
}
