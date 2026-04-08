package ru.clipkeep.service;

import ru.clipkeep.config.AppConfig;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Polls the system clipboard at a fixed interval and notifies a listener
 * whenever the clipboard content changes.
 * <p>
 * Runs on a dedicated daemon thread so it does not prevent JVM shutdown.
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
     * @param clipboardService low-level clipboard access
     * @param config           application configuration (polling interval)
     * @param onNewClip        callback invoked on the scheduler thread when
     *                         clipboard content changes; the new text is passed
     *                         as the argument.
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

    /** Starts the polling loop. Safe to call multiple times; second call is a no-op. */
    public synchronized void start() {
        if (taskHandle != null && !taskHandle.isCancelled()) {
            return;
        }
        long interval = config.getPollingIntervalMs();
        taskHandle = scheduler.scheduleAtFixedRate(
                this::poll, 0, interval, TimeUnit.MILLISECONDS);
        LOGGER.info("ClipboardWatcher started (interval=" + interval + "ms)");
    }

    /** Stops the polling loop. */
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
