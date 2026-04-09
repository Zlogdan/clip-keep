package ru.clipkeep.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import ru.clipkeep.config.AppConfig;
import ru.clipkeep.model.ClipItem;

/**
 * Слой бизнес-логики для истории буфера обмена.
 * <p>
 * Отвечает за:
 * <ul>
 *   <li>Удаление подряд идущих дубликатов</li>
 *   <li>Соблюдение лимита истории (закреплённые элементы никогда не удаляются)</li>
 *   <li>Переключение состояния закрепления</li>
 *   <li>Фильтрацию по тексту</li>
 *   <li>Делегирование сохранения в {@link StorageService}</li>
 * </ul>
 * Все методы синхронизированы для безопасного конкурентного доступа из
 * потока наблюдения за буфером обмена и из потока JavaFX UI.
 */
public class HistoryService {

    private static final Logger LOGGER = Logger.getLogger(HistoryService.class.getName());

    private final StorageService storage;
    private final AppConfig config;

    /** Список в памяти, самые новые элементы находятся по индексу 0. */
    private final List<ClipItem> items;

    public HistoryService(StorageService storage, AppConfig config) {
        this.storage = storage;
        this.config = config;
        this.items = storage.load();
    }

    /**
     * Добавляет новую запись из буфера обмена.
     * Пустой текст и подряд идущие дубликаты игнорируются.
     *
     * @param text текст из буфера обмена для добавления.
     */
    public synchronized void add(String text) {
        if (text == null || text.isBlank()) return;

        // Игнорировать подряд идущий дубликат
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
     * Возвращает неизменяемый снимок всех элементов (сначала новые).
     */
    public synchronized List<ClipItem> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    /**
     * Возвращает элементы, текст которых содержит {@code query} (без учёта регистра).
     * Если {@code query} пустой, возвращаются все элементы.
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
     * Переключает состояние закрепления у элемента с указанным id.
     *
     * @param id id элемента.
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
     * Удаляет элемент с указанным id.
     *
     * @param id id элемента.
     */
    public synchronized void remove(String id) {
        boolean removed = items.removeIf(it -> it.getId().equals(id));
        if (removed) {
            storage.save(new ArrayList<>(items));
            LOGGER.fine("Removed clip id=" + id);
        }
    }

    /**
     * Очищает из истории все незакреплённые элементы.
     */
    public synchronized void clearUnpinned() {
        items.removeIf(Predicate.not(ClipItem::isPinned));
        storage.save(new ArrayList<>(items));
        LOGGER.info("Cleared unpinned history");
    }

    // -----------------------------------------------------------------------
    // Вспомогательные приватные методы
    // -----------------------------------------------------------------------

    /**
     * Удаляет самые старые незакреплённые элементы, пока список
     * не будет соответствовать заданному лимиту.
     */
    private void enforceLimit() {
        int limit = config.getMaxHistorySize();
        long nonPinned = countNonPinned();
        while (nonPinned > limit) {
            // Удаляем самый старый незакреплённый элемент (максимальный индекс)
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
