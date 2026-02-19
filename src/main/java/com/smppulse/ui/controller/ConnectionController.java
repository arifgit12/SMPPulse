package com.smppulse.ui.controller;

import com.smppulse.app.AppContext;
import com.smppulse.config.ConnectionConfig;
import com.smppulse.core.session.SessionEventListener;
import com.smppulse.core.session.SmppSessionWrapper;
import com.smppulse.util.FxUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ConnectionController {

    @FXML private RadioButton esmeModeRadio;
    @FXML private RadioButton smscModeRadio;
    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private TextField systemIdField;
    @FXML private PasswordField passwordField;
    @FXML private TextField systemTypeField;
    @FXML private ComboBox<String> bindTypeCombo;
    @FXML private ComboBox<String> sourceTonCombo;
    @FXML private ComboBox<String> sourceNpiCombo;
    @FXML private ComboBox<String> destTonCombo;
    @FXML private ComboBox<String> destNpiCombo;
    @FXML private Spinner<Integer> sessionCountSpinner;
    @FXML private Spinner<Integer> windowSizeSpinner;
    @FXML private TextField enquireLinkField;
    @FXML private TextField responseTimeoutField;
    @FXML private CheckBox useTlsCheck;
    @FXML private CheckBox autoReconnectCheck;
    @FXML private Button connectButton;
    @FXML private Button disconnectButton;

    private MainController mainController;

    @FXML
    public void initialize() {
        bindTypeCombo.getItems().addAll("TRANSCEIVER", "TRANSMITTER", "RECEIVER");
        bindTypeCombo.setValue("TRANSCEIVER");

        String[] tonValues = {"0 - Unknown", "1 - International", "2 - National",
                "3 - Network Specific", "4 - Subscriber", "5 - Alphanumeric", "6 - Abbreviated"};
        String[] npiValues = {"0 - Unknown", "1 - ISDN (E.164)", "3 - Data (X.121)",
                "4 - Telex", "6 - Land Mobile", "8 - National", "9 - Private", "10 - ERMES", "14 - Internet", "18 - WAP"};

        sourceTonCombo.getItems().addAll(tonValues);
        sourceTonCombo.setValue(tonValues[1]);
        sourceNpiCombo.getItems().addAll(npiValues);
        sourceNpiCombo.setValue(npiValues[1]);
        destTonCombo.getItems().addAll(tonValues);
        destTonCombo.setValue(tonValues[1]);
        destNpiCombo.getItems().addAll(npiValues);
        destNpiCombo.setValue(npiValues[1]);
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void handleConnect() {
        try {
            ConnectionConfig config = buildConfig();
            connectButton.setDisable(true);

            Thread.ofVirtual().name("connect").start(() -> {
                try {
                    AppContext.getInstance().getSessionManager().addListener(new SessionEventListener() {
                        @Override
                        public void onConnectionStateChanged(SmppSessionWrapper session, ConnectionState newState) {
                            FxUtils.runOnFxThread(() -> {
                                if (mainController != null) {
                                    mainController.updateConnectionState(newState);
                                }
                            });
                        }

                        @Override
                        public void onSessionError(SmppSessionWrapper session, Exception error) {
                            FxUtils.runOnFxThread(() ->
                                    FxUtils.showError("Session Error", error.getMessage()));
                        }

                        @Override
                        public void onMessageReceived(SmppSessionWrapper session, String messageId,
                                                       String sourceAddr, String destAddr, String message) {}
                    });

                    AppContext.getInstance().getSessionManager().connectAll(config);
                    FxUtils.runOnFxThread(() -> {
                        connectButton.setDisable(true);
                        disconnectButton.setDisable(false);
                        setFieldsDisabled(true);
                    });
                } catch (Exception e) {
                    FxUtils.runOnFxThread(() -> {
                        connectButton.setDisable(false);
                        FxUtils.showError("Connection Failed", e.getMessage());
                    });
                }
            });
        } catch (Exception e) {
            FxUtils.showError("Invalid Configuration", e.getMessage());
        }
    }

    @FXML
    private void handleDisconnect() {
        AppContext.getInstance().getSessionManager().disconnectAll();
        connectButton.setDisable(false);
        disconnectButton.setDisable(true);
        setFieldsDisabled(false);
        if (mainController != null) {
            mainController.updateConnectionState(SessionEventListener.ConnectionState.DISCONNECTED);
        }
    }

    private void setFieldsDisabled(boolean disabled) {
        hostField.setDisable(disabled);
        portField.setDisable(disabled);
        systemIdField.setDisable(disabled);
        passwordField.setDisable(disabled);
        systemTypeField.setDisable(disabled);
        bindTypeCombo.setDisable(disabled);
        sessionCountSpinner.setDisable(disabled);
        windowSizeSpinner.setDisable(disabled);
        esmeModeRadio.setDisable(disabled);
        smscModeRadio.setDisable(disabled);
    }

    public ConnectionConfig buildConfig() {
        ConnectionConfig config = new ConnectionConfig();
        config.setHost(hostField.getText().trim());
        config.setPort(Integer.parseInt(portField.getText().trim()));
        config.setSystemId(systemIdField.getText().trim());
        config.setPassword(passwordField.getText());
        config.setSystemType(systemTypeField.getText().trim());
        config.setBindType(bindTypeCombo.getValue());
        config.setConnectionMode(esmeModeRadio.isSelected() ? "ESME" : "SMSC");
        config.setSourceAddrTon(extractTonNpi(sourceTonCombo.getValue()));
        config.setSourceAddrNpi(extractTonNpi(sourceNpiCombo.getValue()));
        config.setDestAddrTon(extractTonNpi(destTonCombo.getValue()));
        config.setDestAddrNpi(extractTonNpi(destNpiCombo.getValue()));
        config.setSessionCount(sessionCountSpinner.getValue());
        config.setWindowSize(windowSizeSpinner.getValue());
        config.setEnquireLinkInterval(Integer.parseInt(enquireLinkField.getText().trim()));
        config.setResponseTimeout(Integer.parseInt(responseTimeoutField.getText().trim()));
        config.setUseTls(useTlsCheck.isSelected());
        config.getRetryPolicy().setAutoReconnect(autoReconnectCheck.isSelected());
        return config;
    }

    public void loadConfig(ConnectionConfig config) {
        hostField.setText(config.getHost());
        portField.setText(String.valueOf(config.getPort()));
        systemIdField.setText(config.getSystemId());
        passwordField.setText(config.getPassword());
        systemTypeField.setText(config.getSystemType());
        bindTypeCombo.setValue(config.getBindType());
        esmeModeRadio.setSelected("ESME".equals(config.getConnectionMode()));
        smscModeRadio.setSelected("SMSC".equals(config.getConnectionMode()));
        sessionCountSpinner.getValueFactory().setValue(config.getSessionCount());
        windowSizeSpinner.getValueFactory().setValue(config.getWindowSize());
        enquireLinkField.setText(String.valueOf(config.getEnquireLinkInterval()));
        responseTimeoutField.setText(String.valueOf(config.getResponseTimeout()));
        useTlsCheck.setSelected(config.isUseTls());
        autoReconnectCheck.setSelected(config.getRetryPolicy().isAutoReconnect());
    }

    private byte extractTonNpi(String value) {
        if (value == null) return 0;
        try {
            return Byte.parseByte(value.split(" - ")[0].trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
