package ru.clipkeep.service;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.clipkeep.config.AppConfig;
import ru.clipkeep.model.ClipItem;

/**
 * Тесты для {@link HistoryService}.
 */
class HistoryServiceTest {

    @TempDir
    Path tempDir;

    private HistoryService historyService;

    @BeforeEach
    void setUp() {
        AppConfig config = new AppConfig();
        config.setMaxHistorySize(5);
        config.setStoragePath(tempDir.resolve("history.json").toString());
        StorageService storage = new StorageService(config);
        historyService = new HistoryService(storage, config);
    }

    @Test
    void addItem_shouldBeAvailableInHistory() {
        historyService.add("Hello World");
        List<ClipItem> items = historyService.getAll();
        assertEquals(1, items.size());
        assertEquals("Hello World", items.get(0).getText());
    }

    @Test
    void addBlankText_shouldBeIgnored() {
        historyService.add("");
        historyService.add("   ");
        historyService.add(null);
        assertEquals(0, historyService.getAll().size());
    }

    @Test
    void addConsecutiveDuplicate_shouldBeIgnored() {
        historyService.add("same text");
        historyService.add("same text");
        assertEquals(1, historyService.getAll().size());
    }

    @Test
    void addDifferentItems_shouldAllBePresent() {
        historyService.add("first");
        historyService.add("second");
        historyService.add("third");
        List<ClipItem> items = historyService.getAll();
        assertEquals(3, items.size());
        // Сначала самые новые
        assertEquals("third", items.get(0).getText());
        assertEquals("first", items.get(2).getText());
    }

    @Test
    void historyLimit_shouldEvictOldestUnpinnedItems() {
        for (int i = 1; i <= 7; i++) {
            historyService.add("item " + i);
        }
        // Лимит равен 5; два самых старых элемента должны быть удалены
        assertEquals(5, historyService.getAll().size());
        // Самый новый элемент должен остаться
        assertEquals("item 7", historyService.getAll().get(0).getText());
    }

    @Test
    void pinnedItem_shouldNotBeEvicted() {
        historyService.add("pinned");
        String pinnedId = historyService.getAll().get(0).getId();
        historyService.togglePin(pinnedId);

        // Заполняем историю так, чтобы превысить лимит
        for (int i = 1; i <= 6; i++) {
            historyService.add("item " + i);
        }

        List<ClipItem> all = historyService.getAll();
        boolean pinnedStillPresent = all.stream().anyMatch(it -> it.getId().equals(pinnedId));
        assertTrue(pinnedStillPresent, "Pinned item should not be evicted");
    }

    @Test
    void togglePin_shouldChangePinnedState() {
        historyService.add("test");
        String id = historyService.getAll().get(0).getId();

        assertFalse(historyService.getAll().get(0).isPinned());

        historyService.togglePin(id);
        assertTrue(historyService.getAll().get(0).isPinned());

        historyService.togglePin(id);
        assertFalse(historyService.getAll().get(0).isPinned());
    }

    @Test
    void remove_shouldDeleteItem() {
        historyService.add("remove me");
        String id = historyService.getAll().get(0).getId();
        historyService.remove(id);
        assertEquals(0, historyService.getAll().size());
    }

    @Test
    void clearUnpinned_shouldKeepPinnedItems() {
        historyService.add("regular");
        historyService.add("pinned");
        String pinnedId = historyService.getAll().get(0).getId();
        historyService.togglePin(pinnedId);
        historyService.add("another regular");

        historyService.clearUnpinned();

        List<ClipItem> remaining = historyService.getAll();
        assertEquals(1, remaining.size());
        assertEquals(pinnedId, remaining.get(0).getId());
    }

    @Test
    void search_shouldFilterByText() {
        historyService.add("Hello World");
        historyService.add("Goodbye World");
        historyService.add("Hello Java");

        List<ClipItem> results = historyService.search("hello");
        assertEquals(2, results.size());

        List<ClipItem> allResults = historyService.search("");
        assertEquals(3, allResults.size());
    }

    @Test
    void storageService_shouldPersistAndReload() {
        historyService.add("persistent item");

        // Создаём новый HistoryService с тем же путём хранения — имитируем перезапуск
        AppConfig config2 = new AppConfig();
        config2.setMaxHistorySize(5);
        config2.setStoragePath(tempDir.resolve("history.json").toString());
        StorageService storage2 = new StorageService(config2);
        HistoryService reloaded = new HistoryService(storage2, config2);

        assertEquals(1, reloaded.getAll().size());
        assertEquals("persistent item", reloaded.getAll().get(0).getText());
    }
}
