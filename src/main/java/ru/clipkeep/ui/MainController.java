package ru.clipkeep.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import ru.clipkeep.model.ClipItem;
import ru.clipkeep.service.ClipboardService;
import ru.clipkeep.service.HistoryService;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

/**
 * JavaFX controller for the main application window (main.fxml).
 */
public class MainController {

    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int PREVIEW_LENGTH = 80;

    // -----------------------------------------------------------------------
    // FXML injected fields
    // -----------------------------------------------------------------------

    @FXML private TextField searchField;
    @FXML private ListView<ClipItem> historyList;
    @FXML private TextArea previewArea;
    @FXML private Label timestampLabel;
    @FXML private Button pinButton;
    @FXML private Button deleteButton;
    @FXML private Button clearButton;

    // -----------------------------------------------------------------------
    // Dependencies (set before initialize())
    // -----------------------------------------------------------------------

    private HistoryService historyService;
    private ClipboardService clipboardService;

    /** Called by MainWindow after the FXML is loaded. */
    public void init(HistoryService historyService, ClipboardService clipboardService) {
        this.historyService = historyService;
        this.clipboardService = clipboardService;
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @FXML
    public void initialize() {
        // Cell factory: show truncated text + pin marker
        historyList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ClipItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    String display = truncate(item.getText(), PREVIEW_LENGTH);
                    setText((item.isPinned() ? "📌 " : "") + display);
                    setStyle(item.isPinned() ? "-fx-font-weight: bold;" : "");
                }
            }
        });

        // Selection listener: show full text and enable action buttons
        historyList.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> onSelectionChanged(newVal));

        // Double-click copies to clipboard
        historyList.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                ClipItem selected = historyList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    copyToClipboard(selected);
                }
            }
        });

        // Search field: filter on every keystroke
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter(newVal));

        // Start with buttons disabled
        updateButtonState(null);
    }

    // -----------------------------------------------------------------------
    // Public API called by MainWindow
    // -----------------------------------------------------------------------

    /** Reloads the list from HistoryService and re-applies the current filter. */
    public void refresh() {
        Platform.runLater(() -> applyFilter(searchField.getText()));
    }

    // -----------------------------------------------------------------------
    // FXML action handlers
    // -----------------------------------------------------------------------

    @FXML
    private void onCopy() {
        ClipItem selected = historyList.getSelectionModel().getSelectedItem();
        if (selected != null) copyToClipboard(selected);
    }

    @FXML
    private void onPin() {
        ClipItem selected = historyList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        historyService.togglePin(selected.getId());
        refresh();
    }

    @FXML
    private void onDelete() {
        ClipItem selected = historyList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        historyService.remove(selected.getId());
        previewArea.clear();
        timestampLabel.setText("");
        refresh();
    }

    @FXML
    private void onClear() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Очистить всю историю (кроме закреплённых)?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Подтверждение");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                historyService.clearUnpinned();
                previewArea.clear();
                timestampLabel.setText("");
                refresh();
            }
        });
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void applyFilter(String query) {
        List<ClipItem> filtered = historyService.search(query);
        ObservableList<ClipItem> observable = FXCollections.observableArrayList(filtered);
        historyList.setItems(observable);
    }

    private void onSelectionChanged(ClipItem item) {
        if (item == null) {
            previewArea.clear();
            timestampLabel.setText("");
        } else {
            previewArea.setText(item.getText());
            timestampLabel.setText(item.getTimestamp() != null
                    ? TIMESTAMP_FMT.format(
                            ZonedDateTime.ofInstant(item.getTimestamp(), ZoneId.systemDefault()))
                    : "");
        }
        updateButtonState(item);
    }

    private void copyToClipboard(ClipItem item) {
        clipboardService.writeText(item.getText());
        LOGGER.fine("Copied to clipboard: " + truncate(item.getText(), 40));
    }

    private void updateButtonState(ClipItem selected) {
        boolean hasSelection = selected != null;
        pinButton.setDisable(!hasSelection);
        deleteButton.setDisable(!hasSelection);
        if (hasSelection) {
            pinButton.setText(selected.isPinned() ? "Открепить" : "Закрепить");
        } else {
            pinButton.setText("Закрепить");
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        String singleLine = s.replace('\n', ' ').replace('\r', ' ');
        return singleLine.length() <= maxLen ? singleLine : singleLine.substring(0, maxLen) + "…";
    }
}
