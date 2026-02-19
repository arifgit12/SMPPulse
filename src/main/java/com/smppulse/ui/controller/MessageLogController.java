package com.smppulse.ui.controller;

import com.smppulse.app.AppContext;
import com.smppulse.core.codec.PduRecord;
import com.smppulse.util.FxUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class MessageLogController {

    @FXML private TextField filterField;
    @FXML private ComboBox<String> directionFilter;
    @FXML private CheckBox autoScrollCheck;
    @FXML private Label countLabel;
    @FXML private TableView<PduRecord> messageTable;
    @FXML private TableColumn<PduRecord, String> timestampColumn;
    @FXML private TableColumn<PduRecord, String> directionColumn;
    @FXML private TableColumn<PduRecord, String> pduTypeColumn;
    @FXML private TableColumn<PduRecord, String> messageIdColumn;
    @FXML private TableColumn<PduRecord, String> sourceColumn;
    @FXML private TableColumn<PduRecord, String> destColumn;
    @FXML private TableColumn<PduRecord, String> statusColumn;

    private final ObservableList<PduRecord> allRecords = FXCollections.observableArrayList();
    private FilteredList<PduRecord> filteredRecords;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());
    private static final int MAX_LOG_ENTRIES = 5000;

    @FXML
    public void initialize() {
        timestampColumn.setCellValueFactory(data ->
                new SimpleStringProperty(TIME_FORMAT.format(data.getValue().timestamp())));
        directionColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().direction()));
        pduTypeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().pduType()));
        messageIdColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().messageId()));
        sourceColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().sourceAddr()));
        destColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().destAddr()));
        statusColumn.setCellValueFactory(data -> {
            int status = data.getValue().commandStatus();
            return new SimpleStringProperty(status == 0 ? "OK" : String.format("0x%04X", status));
        });

        // Direction color coding
        directionColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "OUT" -> setStyle("-fx-text-fill: #059669;");
                        case "IN" -> setStyle("-fx-text-fill: #7c3aed;");
                        case "ERR" -> setStyle("-fx-text-fill: #dc2626;");
                        default -> setStyle("");
                    }
                }
            }
        });

        filteredRecords = new FilteredList<>(allRecords);
        messageTable.setItems(filteredRecords);

        directionFilter.setValue("All");

        // Filters
        filterField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        directionFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Register PDU listener
        AppContext.getInstance().getSessionManager().getPduLogger().addRecordListener(this::onNewRecord);
    }

    private void onNewRecord(PduRecord record) {
        FxUtils.runOnFxThread(() -> {
            allRecords.add(record);
            if (allRecords.size() > MAX_LOG_ENTRIES) {
                allRecords.removeFirst();
            }
            countLabel.setText(filteredRecords.size() + " messages");

            if (autoScrollCheck.isSelected() && !messageTable.getItems().isEmpty()) {
                messageTable.scrollTo(messageTable.getItems().size() - 1);
            }
        });
    }

    private void applyFilters() {
        String filterText = filterField.getText() != null ? filterField.getText().toLowerCase() : "";
        String direction = directionFilter.getValue();

        filteredRecords.setPredicate(record -> {
            if (!"All".equals(direction) && !record.direction().equals(direction)) {
                return false;
            }
            if (!filterText.isEmpty()) {
                return record.pduType().toLowerCase().contains(filterText) ||
                       (record.messageId() != null && record.messageId().toLowerCase().contains(filterText)) ||
                       (record.sourceAddr() != null && record.sourceAddr().contains(filterText)) ||
                       (record.destAddr() != null && record.destAddr().contains(filterText));
            }
            return true;
        });
        countLabel.setText(filteredRecords.size() + " messages");
    }

    @FXML
    private void handleClear() {
        allRecords.clear();
        AppContext.getInstance().getSessionManager().getPduLogger().clear();
        countLabel.setText("0 messages");
    }
}
