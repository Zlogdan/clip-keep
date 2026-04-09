package ru.clipkeep.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Низкоуровневый доступ к буферу обмена через консольную утилиту {@code xsel}.
 * <p>
 * Поддерживается только обычный текст (UTF-8). Класс спроектирован так,
 * чтобы в будущем можно было добавить поддержку изображений через
 * дополнительные методы чтения/записи с использованием других флагов
 * xsel/xclip или нативной библиотеки.
 */
public class ClipboardService {

    private static final Logger LOGGER = Logger.getLogger(ClipboardService.class.getName());

    /**
     * Читает текущее текстовое содержимое системного буфера обмена (X11 selection).
     *
     * @return текст из буфера обмена или {@code null}, если xsel недоступен
     *         либо буфер обмена пуст.
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
     * Записывает текст в системный буфер обмена (X11 selection).
     *
     * @param text текст для установки; не должен быть {@code null}.
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
