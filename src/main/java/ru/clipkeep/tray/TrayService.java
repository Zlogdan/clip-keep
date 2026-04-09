package ru.clipkeep.tray;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import javafx.application.Platform;
import ru.clipkeep.service.HistoryService;
import ru.clipkeep.ui.MainWindow;

/**
 * Управляет иконкой в системном трее и её контекстным меню.
 * <p>
 * Использует AWT API {@link SystemTray}, доступный на всех платформах,
 * где поддерживается системный трей. JavaFX и AWT/Swing могут работать
 * вместе при установленном {@code Platform.setImplicitExit(false)},
 * что предотвращает завершение JVM при закрытии JavaFX-окна.
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
     * Устанавливает иконку в трей. Должен вызываться из AWT event dispatch thread.
     *
     * @throws UnsupportedOperationException если платформа не поддерживает системный трей.
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
        // Резервный вариант: рисуем простой цветной квадрат
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
