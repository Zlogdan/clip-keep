package ru.clipkeep.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Low-level clipboard access using the {@code xsel} command-line utility.
 * <p>
 * Only plain text (UTF-8) is supported. The class is designed so that
 * future image support can be added by introducing additional read/write
 * methods that delegate to other xsel/xclip flags or a native library.
 */
public class ClipboardService {

    private static final Logger LOGGER = Logger.getLogger(ClipboardService.class.getName());

    /**
     * Reads the current text content of the system clipboard (X11 clipboard selection).
     *
     * @return clipboard text, or {@code null} if xsel is unavailable or clipboard is empty.
     */
    public String readText() {
        try {
            Process process = new ProcessBuilder("xsel", "-b", "--output")
                    .redirectErrorStream(true)
                    .start();

            String content;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(line);
                }
                content = sb.toString();
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                LOGGER.warning("xsel exited with code " + exitCode);
                return null;
            }
            return content;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to read clipboard via xsel: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Writes text to the system clipboard (X11 clipboard selection).
     *
     * @param text the text to set; must not be null.
     */
    public void writeText(String text) {
        if (text == null) throw new IllegalArgumentException("text must not be null");
        try {
            Process process = new ProcessBuilder("xsel", "-b", "--input")
                    .redirectErrorStream(true)
                    .start();

            process.getOutputStream().write(text.getBytes(StandardCharsets.UTF_8));
            process.getOutputStream().close();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                LOGGER.warning("xsel write exited with code " + exitCode);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to write clipboard via xsel: " + e.getMessage(), e);
        }
    }
}
