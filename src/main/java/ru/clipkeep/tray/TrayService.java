package ru.clipkeep.tray;

import javafx.application.Platform;
import ru.clipkeep.service.HistoryService;
import ru.clipkeep.ui.MainWindow;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the system tray icon and its context menu.
 * <p>
 * Uses the AWT {@link SystemTray} API which is available on all platforms
 * that support a system tray.  JavaFX and AWT/Swing can coexist when
 * {@code Platform.setImplicitExit(false)} is set, preventing the JVM from
 * exiting when the JavaFX window is closed.
 */
public class TrayService {

    private static final Logger LOGGER = Logger.getLogger(TrayService.class.getName());

    private final HistoryService historyService;
    private final MainWindow mainWindow;

    private TrayIcon trayIcon;

    public TrayService(HistoryService historyService, MainWindow mainWindow) {
        this.historyService = historyService;
        this.mainWindow = mainWindow;
    }

    /**
     * Installs the tray icon.  Must be called from the AWT event dispatch thread.
     *
     * @throws UnsupportedOperationException if the platform does not support a system tray.
     */
    public void install() {
        if (!SystemTray.isSupported()) {
            LOGGER.warning("System tray is not supported on this platform");
            return;
        }

        SystemTray tray = SystemTray.getSystemTray();
        Image icon = loadIcon();

        PopupMenu popup = new PopupMenu();

        MenuItem openItem = new MenuItem("Открыть");
        openItem.addActionListener(e -> Platform.runLater(mainWindow::show));

        MenuItem clearItem = new MenuItem("Очистить историю");
        clearItem.addActionListener(e -> {
            historyService.clearUnpinned();
            Platform.runLater(mainWindow::refresh);
        });

        MenuItem exitItem = new MenuItem("Выход");
        exitItem.addActionListener(e -> {
            tray.remove(trayIcon);
            Platform.exit();
            System.exit(0);
        });

        popup.add(openItem);
        popup.addSeparator();
        popup.add(clearItem);
        popup.addSeparator();
        popup.add(exitItem);

        trayIcon = new TrayIcon(icon, "ClipKeep", popup);
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(e -> Platform.runLater(mainWindow::show));

        try {
            tray.add(trayIcon);
            LOGGER.info("Tray icon installed");
        } catch (AWTException e) {
            LOGGER.log(Level.SEVERE, "Failed to install tray icon", e);
        }
    }

    // -----------------------------------------------------------------------

    private Image loadIcon() {
        try (InputStream is = getClass().getResourceAsStream("/images/icon.png")) {
            if (is != null) {
                return ImageIO.read(is);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load tray icon", e);
        }
        // Fallback: draw a simple coloured square
        return createFallbackIcon();
    }

    private Image createFallbackIcon() {
        int size = 16;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(0x3D7AB5));
        g.fillRect(0, 0, size, size);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        g.drawString("C", 3, 12);
        g.dispose();
        return img;
    }
}
