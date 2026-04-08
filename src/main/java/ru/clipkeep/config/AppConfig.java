package ru.clipkeep.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Application configuration loaded from config.json.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfig {

    private int maxHistorySize = 100;
    private long pollingIntervalMs = 500;
    private String storagePath = "history.json";

    public AppConfig() {}

    public int getMaxHistorySize() {
        return maxHistorySize;
    }

    public void setMaxHistorySize(int maxHistorySize) {
        this.maxHistorySize = maxHistorySize;
    }

    public long getPollingIntervalMs() {
        return pollingIntervalMs;
    }

    public void setPollingIntervalMs(long pollingIntervalMs) {
        this.pollingIntervalMs = pollingIntervalMs;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }
}
