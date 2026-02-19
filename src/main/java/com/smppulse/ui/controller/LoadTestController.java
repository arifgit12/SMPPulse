package com.smppulse.ui.controller;

import com.smppulse.app.AppContext;
import com.smppulse.config.LoadTestConfig;
import com.smppulse.generator.LoadTestState;
import com.smppulse.util.FxUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoadTestController {

    @FXML private Spinner<Integer> targetTpsSpinner;
    @FXML private Spinner<Integer> rampUpSpinner;
    @FXML private Spinner<Integer> rampDownSpinner;
    @FXML private Spinner<Integer> durationSpinner;
    @FXML private Spinner<Integer> totalMessagesSpinner;
    @FXML private ComboBox<String> operationCombo;
    @FXML private TextField serviceTypeField;
    @FXML private Spinner<Integer> dataCodingSpinner;
    @FXML private Spinner<Integer> protocolIdSpinner;
    @FXML private Spinner<Integer> priorityFlagSpinner;
    @FXML private Spinner<Integer> maxInFlightSpinner;
    @FXML private Button startButton;
    @FXML private Button pauseButton;
    @FXML private Button stopButton;

    private MainController mainController;

    @FXML
    public void initialize() {
        operationCombo.getItems().addAll("SUBMIT_SM", "DATA_SM");
        operationCombo.setValue("SUBMIT_SM");
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void handleStart() {
        try {
            if (!AppContext.getInstance().getSessionManager().hasConnectedSession()) {
                FxUtils.showError("Not Connected", "Please connect to an SMPP server first.");
                return;
            }

            if (mainController != null) {
                mainController.startLoadTest();
            }

            startButton.setDisable(true);
            pauseButton.setDisable(false);
            stopButton.setDisable(false);
        } catch (Exception e) {
            FxUtils.showError("Start Failed", e.getMessage());
        }
    }

    @FXML
    private void handlePause() {
        LoadTestState state = AppContext.getInstance().getLoadEngine().getState();
        if (state == LoadTestState.RUNNING) {
            AppContext.getInstance().getLoadEngine().pause();
            pauseButton.setText("Resume");
        } else if (state == LoadTestState.PAUSED) {
            AppContext.getInstance().getLoadEngine().resume();
            pauseButton.setText("Pause");
        }
    }

    @FXML
    private void handleStop() {
        AppContext.getInstance().getLoadEngine().stop();
        resetButtons();
    }

    public void resetButtons() {
        startButton.setDisable(false);
        pauseButton.setDisable(true);
        pauseButton.setText("Pause");
        stopButton.setDisable(true);
    }

    public void onTestStateChanged(LoadTestState state) {
        FxUtils.runOnFxThread(() -> {
            switch (state) {
                case RUNNING -> {
                    startButton.setDisable(true);
                    pauseButton.setDisable(false);
                    stopButton.setDisable(false);
                }
                case PAUSED -> pauseButton.setText("Resume");
                case STOPPED, IDLE -> resetButtons();
                default -> {}
            }
        });
    }

    public LoadTestConfig buildConfig() {
        LoadTestConfig config = new LoadTestConfig();
        config.setTargetTps(targetTpsSpinner.getValue());
        config.setRampUpSeconds(rampUpSpinner.getValue());
        config.setRampDownSeconds(rampDownSpinner.getValue());
        config.setDurationSeconds(durationSpinner.getValue());
        config.setTotalMessages(totalMessagesSpinner.getValue());
        config.setOperationType(operationCombo.getValue());
        config.setServiceType(serviceTypeField.getText().trim());
        config.setDataCodingValue(dataCodingSpinner.getValue().byteValue());
        config.setProtocolId(protocolIdSpinner.getValue().byteValue());
        config.setPriorityFlag(priorityFlagSpinner.getValue().byteValue());
        config.setMaxInFlight(maxInFlightSpinner.getValue());
        return config;
    }

    public void loadConfig(LoadTestConfig config) {
        targetTpsSpinner.getValueFactory().setValue(config.getTargetTps());
        rampUpSpinner.getValueFactory().setValue(config.getRampUpSeconds());
        rampDownSpinner.getValueFactory().setValue(config.getRampDownSeconds());
        durationSpinner.getValueFactory().setValue(config.getDurationSeconds());
        totalMessagesSpinner.getValueFactory().setValue(config.getTotalMessages());
        operationCombo.setValue(config.getOperationType());
        serviceTypeField.setText(config.getServiceType());
        dataCodingSpinner.getValueFactory().setValue((int) config.getDataCodingValue());
        protocolIdSpinner.getValueFactory().setValue((int) config.getProtocolId());
        priorityFlagSpinner.getValueFactory().setValue((int) config.getPriorityFlag());
        maxInFlightSpinner.getValueFactory().setValue(config.getMaxInFlight());
    }
}
