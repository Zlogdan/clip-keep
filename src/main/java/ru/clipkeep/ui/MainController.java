package ru.clipkeep.ui;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import ru.clipkeep.model.ClipItem;
import ru.clipkeep.service.ClipboardService;
import ru.clipkeep.service.HistoryService;

/**
 * JavaFX-контроллер главного окна приложения (main.fxml).
 */
public class MainController {

    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int PREVIEW_LENGTH = 80;

    // -----------------------------------------------------------------------
    // Поля, внедряемые из FXML
    // -----------------------------------------------------------------------

    @FXML private TextField searchField;
    @FXML private ListView<ClipItem> historyList;
    @FXML private TextArea previewArea;
    @FXML private Label timestampLabel;
    @FXML private Button pinButton;
    @FXML private Button deleteButton;
    @FXML private Button clearButton;

    // -----------------------------------------------------------------------
    // Зависимости (задаются до initialize())
    // -----------------------------------------------------------------------

    private HistoryService historyService;
    private ClipboardService clipboardService;

    /** Вызывается из MainWindow после загрузки FXML. */
    public void init(HistoryService historyService, ClipboardService clipboardService) {
        this.historyService = historyService;
        this.clipboardService = clipboardService;
    }

    // -----------------------------------------------------------------------
    // Жизненный цикл
    // -----------------------------------------------------------------------

    @FXML
    public void initialize() {
        // Фабрика ячеек: показываем сокращённый текст + маркер закрепления
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

        // Слушатель выбора: показываем полный текст и активируем кнопки действий
        historyList.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> onSelectionChanged(newVal));

        // Двойной клик копирует элемент в буфер обмена
        historyList.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                ClipItem selected = historyList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    copyToClipboard(selected);
                }
            }
        });

        // Поле поиска: фильтрация при каждом вводе символа
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter(newVal));

        // В начале кнопки отключены
        updateButtonState(null);
    }

    // -----------------------------------------------------------------------
    // Публичный API, вызываемый MainWindow
    // -----------------------------------------------------------------------

    /** Перезагружает список из HistoryService и заново применяет текущий фильтр. */
    public void refresh() {
        Platform.runLater(() -> applyFilter(searchField.getText()));
    }

    // -----------------------------------------------------------------------
    // Обработчики действий FXML
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
    // Приватные вспомогательные методы
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
        minimizeWindow();
    }

    private void minimizeWindow() {
        if (historyList.getScene() == null || historyList.getScene().getWindow() == null) {
            return;
        }
        if (historyList.getScene().getWindow() instanceof Stage stage) {
            stage.setIconified(true);
        }
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
