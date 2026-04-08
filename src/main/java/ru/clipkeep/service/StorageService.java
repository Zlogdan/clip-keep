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
 * Reads and writes the clipboard history to a JSON file.
 * <p>
 * The file is located at {@link AppConfig#getStoragePath()}.  If the path
 * is relative it is resolved against the current working directory.
 * <p>
 * All public methods are synchronised so that the background watcher thread
 * and the JavaFX thread can both call them safely.
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
     * Loads all history items from disk.
     *
     * @return mutable list of items (newest first), empty list if file absent.
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
     * Persists the given list to disk, overwriting the previous file.
     *
     * @param items list to save (order is preserved).
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
