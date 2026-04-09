package ru.clipkeep.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.clipkeep.config.AppConfig;

/**
 * Опрос системного буфера обмена с фиксированным интервалом и уведомление
 * слушателя при каждом изменении содержимого.
 * <p>
 * Работает в отдельном daemon-потоке и не препятствует завершению JVM.
 */
public class ClipboardWatcher {

    private static final Logger LOGGER = Logger.getLogger(ClipboardWatcher.class.getName());

    private final ClipboardService clipboardService;
    private final AppConfig config;
    private final Consumer<String> onNewClip;

    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> taskHandle;
    private volatile String lastSeen = null;

    /**
     * @param clipboardService низкоуровневый доступ к буферу обмена
     * @param config           конфигурация приложения (интервал опроса)
     * @param onNewClip        колбэк, вызываемый в потоке планировщика при
     *                         изменении содержимого буфера; в аргумент
     *                         передаётся новый текст.
     */
    public ClipboardWatcher(ClipboardService clipboardService,
                            AppConfig config,
                            Consumer<String> onNewClip) {
        this.clipboardService = clipboardService;
        this.config = config;
        this.onNewClip = onNewClip;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ClipboardWatcher");
            t.setDaemon(true);
            return t;
        });
    }

    /** Запускает цикл опроса. Повторный вызов безопасен и ничего не делает. */
    public synchronized void start() {
        if (taskHandle != null && !taskHandle.isCancelled()) {
            return;
        }
        long interval = config.getPollingIntervalMs();
        taskHandle = scheduler.scheduleAtFixedRate(
                this::poll, 0, interval, TimeUnit.MILLISECONDS);
        LOGGER.info("ClipboardWatcher started (interval=" + interval + "ms)");
    }

    /** Останавливает цикл опроса. */
    public synchronized void stop() {
        if (taskHandle != null) {
            taskHandle.cancel(false);
            taskHandle = null;
        }
        scheduler.shutdown();
        LOGGER.info("ClipboardWatcher stopped");
    }

    private void poll() {
        try {
            String current = clipboardService.readText();
            if (current == null || current.isBlank()) {
                return;
            }
            if (!current.equals(lastSeen)) {
                lastSeen = current;
                onNewClip.accept(current);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during clipboard poll", e);
        }
    }
}
