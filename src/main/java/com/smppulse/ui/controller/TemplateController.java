package com.smppulse.ui.controller;

import com.smppulse.app.AppContext;
import com.smppulse.config.MessageTemplate;
import com.smppulse.util.FxUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.Map;

public class TemplateController {

    @FXML private TextField sourceAddrField;
    @FXML private TextField destAddrField;
    @FXML private TextArea messageTextArea;
    @FXML private VBox placeholderRefBox;
    @FXML private Label previewSourceLabel;
    @FXML private Label previewDestLabel;
    @FXML private Label previewMessageLabel;
    @FXML private TextField tlvTagField;
    @FXML private TextField tlvValueField;
    @FXML private TableView<Map.Entry<String, String>> tlvTable;
    @FXML private TableColumn<Map.Entry<String, String>, String> tlvTagColumn;
    @FXML private TableColumn<Map.Entry<String, String>, String> tlvValueColumn;
    @FXML private TableColumn<Map.Entry<String, String>, String> tlvActionColumn;

    private final ObservableList<Map.Entry<String, String>> tlvEntries = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Set default template values (can't use ${} in FXML - it's treated as binding expression)
        sourceAddrField.setText("${random.digits(10)}");
        destAddrField.setText("${random.digits(10)}");
        messageTextArea.setText("Test message ${sequence} at ${timestamp}");

        // Populate placeholder reference
        String[] refs = {
                "${random.digits(n)} - Random digits of length n",
                "${random.alpha(n)} - Random letters of length n",
                "${random.alphanumeric(n)} - Random alphanumeric",
                "${sequence} - Auto-incrementing number",
                "${uuid} - Random UUID",
                "${timestamp} - Current epoch millis",
                "${timestamp:yyyyMMddHHmmss} - Formatted timestamp",
                "${range(min,max)} - Random in range",
                "${file:path:random} - Line from file",
                "${csv:path:column} - Value from CSV"
        };
        for (String ref : refs) {
            Label label = new Label(ref);
            label.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");
            placeholderRefBox.getChildren().add(label);
        }

        // TLV table setup
        tlvTagColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getKey()));
        tlvValueColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getValue()));
        tlvActionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button deleteBtn = new Button("X");
            {
                deleteBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-size: 10; -fx-padding: 2 6;");
                deleteBtn.setOnAction(e -> {
                    Map.Entry<String, String> entry = getTableView().getItems().get(getIndex());
                    tlvEntries.remove(entry);
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });
        tlvTable.setItems(tlvEntries);
    }

    @FXML
    private void handleResolvePreview() {
        try {
            var resolver = AppContext.getInstance().getParameterResolver();
            String resolvedSource = resolver.resolveTemplate(sourceAddrField.getText());
            String resolvedDest = resolver.resolveTemplate(destAddrField.getText());
            String resolvedMessage = resolver.resolveTemplate(messageTextArea.getText());

            previewSourceLabel.setText("Source: " + resolvedSource);
            previewDestLabel.setText("Dest: " + resolvedDest);
            previewMessageLabel.setText("Message: " + resolvedMessage);
        } catch (Exception e) {
            FxUtils.showError("Template Error", e.getMessage());
        }
    }

    @FXML
    private void handleAddTlv() {
        String tag = tlvTagField.getText().trim();
        String value = tlvValueField.getText().trim();
        if (tag.isEmpty() || value.isEmpty()) {
            FxUtils.showError("Invalid TLV", "Both tag and value are required.");
            return;
        }
        tlvEntries.add(Map.entry(tag, value));
        tlvTagField.clear();
        tlvValueField.clear();
    }

    public MessageTemplate buildTemplate() {
        MessageTemplate template = new MessageTemplate();
        template.setSourceAddress(sourceAddrField.getText());
        template.setDestinationAddress(destAddrField.getText());
        template.setMessageText(messageTextArea.getText());
        for (Map.Entry<String, String> entry : tlvEntries) {
            template.getTlvValues().put(entry.getKey(), entry.getValue());
        }
        return template;
    }

    public void loadTemplate(MessageTemplate template) {
        sourceAddrField.setText(template.getSourceAddress());
        destAddrField.setText(template.getDestinationAddress());
        messageTextArea.setText(template.getMessageText());
        tlvEntries.clear();
        if (template.getTlvValues() != null) {
            tlvEntries.addAll(template.getTlvValues().entrySet());
        }
    }
}
