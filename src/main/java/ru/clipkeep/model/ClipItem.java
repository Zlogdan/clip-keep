package ru.clipkeep.model;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Представляет одну запись истории буфера обмена.
 * Спроектирована так, чтобы её было легко расширить для будущей поддержки изображений.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClipItem {

    private String id;
    private String text;
    private Instant timestamp;
    private boolean pinned;

    /** Конструктор по умолчанию, необходимый для Jackson. */
    public ClipItem() {}

    public ClipItem(String text) {
        this.id = UUID.randomUUID().toString();
        this.text = text;
        this.timestamp = Instant.now();
        this.pinned = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    @Override
    public String toString() {
        return "ClipItem{id='" + id + "', pinned=" + pinned
                + ", text='" + truncate(text, 60) + "'}";
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "…";
    }
}
