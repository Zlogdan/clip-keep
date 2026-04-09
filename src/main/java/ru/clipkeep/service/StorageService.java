package ru.clipkeep.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ru.clipkeep.config.AppConfig;
import ru.clipkeep.model.ClipItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Читает и записывает историю буфера обмена в JSON-файл.
 * <p>
 * Файл находится по пути из {@link AppConfig#getStoragePath()}. Если путь
 * относительный, он вычисляется относительно текущего рабочего каталога.
 * <p>
 * Все публичные методы синхронизированы, чтобы их безопасно могли вызывать
 * и фоновый поток наблюдения, и поток JavaFX.
 */
public class StorageService {

    private static final Logger LOGGER = Logger.getLogger(StorageService.class.getName());

    private final ObjectMapper mapper;
    private final Path storagePath;

    public StorageService(AppConfig config) {
        this.storagePath = Paths.get(config.getStoragePath());
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Загружает все элементы истории с диска.
     *
     * @return изменяемый список элементов (сначала новые);
     *         пустой список, если файл отсутствует.
     */
    public synchronized List<ClipItem> load() {
        if (!Files.exists(storagePath)) {
            return new ArrayList<>();
        }
        try {
            List<ClipItem> items = mapper.readValue(
                    storagePath.toFile(),
                    new TypeReference<List<ClipItem>>() {});
            return items != null ? new ArrayList<>(items) : new ArrayList<>();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load history from " + storagePath, e);
            return new ArrayList<>();
        }
    }

    /**
     * Сохраняет переданный список на диск, перезаписывая предыдущий файл.
     *
     * @param items список для сохранения (порядок сохраняется).
     */
    public synchronized void save(List<ClipItem> items) {
        try {
            Path parent = storagePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(storagePath.toFile(), items);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save history to " + storagePath, e);
        }
    }
}
