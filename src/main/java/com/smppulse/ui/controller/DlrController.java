package com.smppulse.ui.controller;

import com.smppulse.config.DlrConfig;
import com.smppulse.metrics.MetricsSnapshot;
import com.smppulse.util.FxUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class DlrController {

    @FXML private CheckBox requestDlrCheck;
    @FXML private ComboBox<String> registeredDeliveryCombo;
    @FXML private Spinner<Integer> dlrTimeoutSpinner;
    @FXML private CheckBox generateDlrCheck;
    @FXML private Spinner<Integer> dlrDelaySpinner;
    @FXML private Slider successRateSlider;
    @FXML private Label successRateLabel;
    @FXML private TextArea dlrFormatArea;
    @FXML private Label dlrReceivedLabel;
    @FXML private Label dlrPendingLabel;
    @FXML private Label dlrTimedOutLabel;
    @FXML private Label dlrSuccessLabel;
    @FXML private Label dlrFailedLabel;

    @FXML
    public void initialize() {
        registeredDeliveryCombo.getItems().addAll(
                "0x01 - Success/Failure",
                "0x02 - Failure Only",
                "0x03 - Success Only"
        );
        registeredDeliveryCombo.setValue("0x01 - Success/Failure");

        successRateSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                successRateLabel.setText(String.format("%.0f%%", newVal.doubleValue() * 100)));
    }

    public void updateStats(MetricsSnapshot snapshot) {
        FxUtils.runOnFxThread(() -> {
            dlrReceivedLabel.setText(String.valueOf(snapshot.dlrReceived()));
            dlrPendingLabel.setText(String.valueOf(snapshot.dlrPending()));
            dlrTimedOutLabel.setText(String.valueOf(snapshot.dlrTimedOut()));
            dlrSuccessLabel.setText(String.valueOf(snapshot.dlrSuccess()));
            dlrFailedLabel.setText(String.valueOf(snapshot.dlrFailed()));
        });
    }

    public DlrConfig buildConfig() {
        DlrConfig config = new DlrConfig();
        config.setRequestDlr(requestDlrCheck.isSelected());
        config.setRegisteredDelivery(extractRegisteredDelivery());
        config.setDlrTimeoutSeconds(dlrTimeoutSpinner.getValue());
        config.setGenerateDlr(generateDlrCheck.isSelected());
        config.setDlrDelayMs(dlrDelaySpinner.getValue());
        config.setDlrSuccessRate(successRateSlider.getValue());
        config.setDlrFormat(dlrFormatArea.getText());
        return config;
    }

    public void loadConfig(DlrConfig config) {
        requestDlrCheck.setSelected(config.isRequestDlr());
        dlrTimeoutSpinner.getValueFactory().setValue(config.getDlrTimeoutSeconds());
        generateDlrCheck.setSelected(config.isGenerateDlr());
        dlrDelaySpinner.getValueFactory().setValue(config.getDlrDelayMs());
        successRateSlider.setValue(config.getDlrSuccessRate());
        dlrFormatArea.setText(config.getDlrFormat());
    }

    private byte extractRegisteredDelivery() {
        String value = registeredDeliveryCombo.getValue();
        if (value == null) return 0x01;
        if (value.startsWith("0x02")) return 0x02;
        if (value.startsWith("0x03")) return 0x03;
        return 0x01;
    }
}
