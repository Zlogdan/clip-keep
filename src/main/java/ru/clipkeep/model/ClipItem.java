package ru.clipkeep.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a single clipboard history entry.
 * Designed to be easily extensible for future image support.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClipItem {

    private String id;
    private String text;
    private Instant timestamp;
    private boolean pinned;

    /** Default constructor required by Jackson. */
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
