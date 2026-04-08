package ru.clipkeep.service;

import ru.clipkeep.config.AppConfig;
import ru.clipkeep.model.ClipItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Business-logic layer for the clipboard history.
 * <p>
 * Responsible for:
 * <ul>
 *   <li>Deduplication of consecutive identical entries</li>
 *   <li>Enforcing the history size limit (pinned items are never removed)</li>
 *   <li>Pin / unpin toggling</li>
 *   <li>Text-based filtering</li>
 *   <li>Delegating persistence to {@link StorageService}</li>
 * </ul>
 * All methods are synchronised to be safe for concurrent access from the
 * clipboard-watcher thread and the JavaFX UI thread.
 */
public class HistoryService {

    private static final Logger LOGGER = Logger.getLogger(HistoryService.class.getName());

    private final StorageService storage;
    private final AppConfig config;

    /** In-memory list, newest items at index 0. */
    private final List<ClipItem> items;

    public HistoryService(StorageService storage, AppConfig config) {
        this.storage = storage;
        this.config = config;
        this.items = storage.load();
    }

    /**
     * Adds a new clipboard entry.
     * Ignores blank text and consecutive duplicates.
     *
     * @param text clipboard text to add.
     */
    public synchronized void add(String text) {
        if (text == null || text.isBlank()) return;

        // Ignore consecutive duplicate
        if (!items.isEmpty() && text.equals(items.get(0).getText())) {
            return;
        }

        ClipItem item = new ClipItem(text);
        items.add(0, item);

        enforceLimit();
        storage.save(new ArrayList<>(items));
        LOGGER.fine("Added clip: " + item);
    }

    /**
     * Returns an unmodifiable snapshot of all items (newest first).
     */
    public synchronized List<ClipItem> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    /**
     * Returns items whose text contains {@code query} (case-insensitive).
     * Returns all items when {@code query} is blank.
     */
    public synchronized List<ClipItem> search(String query) {
        if (query == null || query.isBlank()) {
            return getAll();
        }
        String lower = query.toLowerCase();
        return items.stream()
                .filter(it -> it.getText() != null && it.getText().toLowerCase().contains(lower))
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Toggles the pinned state of the item with the given id.
     *
     * @param id item id.
     */
    public synchronized void togglePin(String id) {
        items.stream()
                .filter(it -> it.getId().equals(id))
                .findFirst()
                .ifPresent(it -> {
                    it.setPinned(!it.isPinned());
                    storage.save(new ArrayList<>(items));
                    LOGGER.fine("Toggled pin for " + id + " -> " + it.isPinned());
                });
    }

    /**
     * Removes the item with the given id.
     *
     * @param id item id.
     */
    public synchronized void remove(String id) {
        boolean removed = items.removeIf(it -> it.getId().equals(id));
        if (removed) {
            storage.save(new ArrayList<>(items));
            LOGGER.fine("Removed clip id=" + id);
        }
    }

    /**
     * Clears all non-pinned items from the history.
     */
    public synchronized void clearUnpinned() {
        items.removeIf(Predicate.not(ClipItem::isPinned));
        storage.save(new ArrayList<>(items));
        LOGGER.info("Cleared unpinned history");
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Removes oldest non-pinned items until the list fits within the configured limit.
     */
    private void enforceLimit() {
        int limit = config.getMaxHistorySize();
        long nonPinned = countNonPinned();
        while (nonPinned > limit) {
            // Remove oldest non-pinned item (highest index)
            for (int i = items.size() - 1; i >= 0; i--) {
                if (!items.get(i).isPinned()) {
                    items.remove(i);
                    nonPinned--;
                    break;
                }
            }
        }
    }

    private long countNonPinned() {
        return items.stream().filter(Predicate.not(ClipItem::isPinned)).count();
    }
}
