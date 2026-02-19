package com.smppulse.ui.controller;

import com.smppulse.app.AppContext;
import com.smppulse.config.DlrConfig;
import com.smppulse.config.LoadTestConfig;
import com.smppulse.config.MessageTemplate;
import com.smppulse.config.TestProfile;
import com.smppulse.core.session.SessionEventListener.ConnectionState;
import com.smppulse.generator.LoadTestState;
import com.smppulse.metrics.MetricsSnapshot;
import com.smppulse.util.FxUtils;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Path;

public class MainController {

    @FXML private SplitPane mainSplitPane;
    @FXML private TabPane configTabPane;
    @FXML private TabPane monitorTabPane;
    @FXML private MenuItem startMenuItem;
    @FXML private MenuItem pauseMenuItem;
    @FXML private MenuItem stopMenuItem;

    // Status bar
    @FXML private HBox statusBar;
    @FXML private Region connectionIndicator;
    @FXML private Label connectionStatusLabel;
    @FXML private Label tpsLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private Label elapsedLabel;

    // Included controllers (injected by fx:include + fx:id naming convention)
    @FXML private ConnectionController connectionPanelController;
    @FXML private LoadTestController loadTestPanelController;
    @FXML private DlrController dlrPanelController;
    @FXML private TemplateController templatePanelController;
    @FXML private DashboardController dashboardPanelController;
    @FXML private MessageLogController messageLogPanelController;

    private Timeline metricsTimeline;

    @FXML
    public void initialize() {
        // Wire child controllers to main
        connectionPanelController.setMainController(this);
        loadTestPanelController.setMainController(this);

        // Register load engine state listener
        AppContext.getInstance().getLoadEngine().addStateListener(state ->
                FxUtils.runOnFxThread(() -> onLoadTestStateChanged(state)));

        // Start metrics refresh timeline (2 Hz)
        metricsTimeline = new Timeline(new KeyFrame(Duration.millis(500), e -> refreshMetrics()));
        metricsTimeline.setCycleCount(Timeline.INDEFINITE);
        metricsTimeline.play();
    }

    private void refreshMetrics() {
        MetricsSnapshot snapshot = AppContext.getInstance().getMetricsCollector().snapshot();

        // Update dashboard
        dashboardPanelController.updateMetrics(snapshot);

        // Update DLR stats
        dlrPanelController.updateStats(snapshot);

        // Update status bar
        tpsLabel.setText(String.format("TPS: %.0f", snapshot.currentTps()));

        // Update progress
        var progress = AppContext.getInstance().getLoadEngine().getProgress();
        double pct = progress.progressPercent();
        progressBar.setProgress(pct / 100.0);
        progressLabel.setText(String.format("%.1f%%", pct));

        // Update elapsed time
        java.time.Duration elapsed = snapshot.elapsed();
        long hours = elapsed.toHours();
        long minutes = elapsed.toMinutesPart();
        long seconds = elapsed.toSecondsPart();
        elapsedLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }

    public void updateConnectionState(ConnectionState state) {
        FxUtils.runOnFxThread(() -> {
            connectionIndicator.getStyleClass().removeAll("disconnected", "connecting", "connected", "error");
            switch (state) {
                case DISCONNECTED -> {
                    connectionIndicator.getStyleClass().add("disconnected");
                    connectionStatusLabel.setText("Disconnected");
                }
                case CONNECTING, RECONNECTING -> {
                    connectionIndicator.getStyleClass().add("connecting");
                    connectionStatusLabel.setText("Connecting...");
                }
                case CONNECTED -> {
                    connectionIndicator.getStyleClass().add("connected");
                    int count = AppContext.getInstance().getSessionManager().getConnectedCount();
                    int total = AppContext.getInstance().getSessionManager().getTotalCount();
                    connectionStatusLabel.setText(String.format("Connected (%d/%d)", count, total));
                }
                case ERROR -> {
                    connectionIndicator.getStyleClass().add("error");
                    connectionStatusLabel.setText("Error");
                }
            }
        });
    }

    public void startLoadTest() {
        LoadTestConfig loadTestConfig = loadTestPanelController.buildConfig();
        MessageTemplate template = templatePanelController.buildTemplate();
        DlrConfig dlrConfig = dlrPanelController.buildConfig();

        dashboardPanelController.reset();

        AppContext.getInstance().getLoadEngine().start(loadTestConfig, template, dlrConfig);
    }

    private void onLoadTestStateChanged(LoadTestState state) {
        loadTestPanelController.onTestStateChanged(state);

        switch (state) {
            case RUNNING -> {
                startMenuItem.setDisable(true);
                pauseMenuItem.setDisable(false);
                stopMenuItem.setDisable(false);
            }
            case PAUSED -> {
                pauseMenuItem.setText("Resume");
            }
            case STOPPED, IDLE -> {
                startMenuItem.setDisable(false);
                pauseMenuItem.setDisable(true);
                pauseMenuItem.setText("Pause");
                stopMenuItem.setDisable(true);
            }
            default -> {}
        }
    }

    // === Menu Handlers ===

    @FXML
    private void handleLoadProfile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load Profile");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("YAML Files", "*.yaml", "*.yml"));
        File file = chooser.showOpenDialog(mainSplitPane.getScene().getWindow());
        if (file != null) {
            try {
                TestProfile profile = AppContext.getInstance().getProfileManager().loadProfile(file.toPath());
                connectionPanelController.loadConfig(profile.getConnection());
                loadTestPanelController.loadConfig(profile.getLoadTest());
                dlrPanelController.loadConfig(profile.getDlr());
                templatePanelController.loadTemplate(profile.getMessageTemplate());
                FxUtils.showInfo("Profile Loaded", "Profile loaded from: " + file.getName());
            } catch (Exception e) {
                FxUtils.showError("Load Failed", e.getMessage());
            }
        }
    }

    @FXML
    private void handleSaveProfile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Profile");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("YAML Files", "*.yaml", "*.yml"));
        chooser.setInitialFileName("profile.yaml");
        File file = chooser.showSaveDialog(mainSplitPane.getScene().getWindow());
        if (file != null) {
            try {
                TestProfile profile = buildCurrentProfile();
                AppContext.getInstance().getProfileManager().setCurrentProfile(profile);
                AppContext.getInstance().getProfileManager().saveProfile(file.toPath());
                FxUtils.showInfo("Profile Saved", "Profile saved to: " + file.getName());
            } catch (Exception e) {
                FxUtils.showError("Save Failed", e.getMessage());
            }
        }
    }

    @FXML
    private void handleExportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Results (CSV)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        chooser.setInitialFileName("results.csv");
        File file = chooser.showSaveDialog(mainSplitPane.getScene().getWindow());
        if (file != null) {
            try {
                AppContext.getInstance().getResultExporter().exportCsv(file.toPath());
                FxUtils.showInfo("Export Complete", "Results exported to: " + file.getName());
            } catch (Exception e) {
                FxUtils.showError("Export Failed", e.getMessage());
            }
        }
    }

    @FXML
    private void handleExportJson() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Results (JSON)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        chooser.setInitialFileName("results.json");
        File file = chooser.showSaveDialog(mainSplitPane.getScene().getWindow());
        if (file != null) {
            try {
                AppContext.getInstance().getResultExporter().exportJson(file.toPath());
                FxUtils.showInfo("Export Complete", "Results exported to: " + file.getName());
            } catch (Exception e) {
                FxUtils.showError("Export Failed", e.getMessage());
            }
        }
    }

    @FXML
    private void handleExportHtml() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Results (HTML)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML Files", "*.html"));
        chooser.setInitialFileName("results.html");
        File file = chooser.showSaveDialog(mainSplitPane.getScene().getWindow());
        if (file != null) {
            try {
                AppContext.getInstance().getResultExporter().exportHtml(file.toPath());
                FxUtils.showInfo("Export Complete", "Results exported to: " + file.getName());
            } catch (Exception e) {
                FxUtils.showError("Export Failed", e.getMessage());
            }
        }
    }

    @FXML
    private void handleStartTest() {
        startLoadTest();
    }

    @FXML
    private void handlePauseTest() {
        LoadTestState state = AppContext.getInstance().getLoadEngine().getState();
        if (state == LoadTestState.RUNNING) {
            AppContext.getInstance().getLoadEngine().pause();
        } else if (state == LoadTestState.PAUSED) {
            AppContext.getInstance().getLoadEngine().resume();
        }
    }

    @FXML
    private void handleStopTest() {
        AppContext.getInstance().getLoadEngine().stop();
    }

    @FXML
    private void handleResetMetrics() {
        AppContext.getInstance().getMetricsCollector().reset();
        dashboardPanelController.reset();
    }

    @FXML
    private void handleExit() {
        AppContext.getInstance().shutdown();
        System.exit(0);
    }

    @FXML
    private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About SMPPulse");
        alert.setHeaderText("SMPPulse v1.0.0");
        alert.setContentText(
                "SMPP Server Load Testing Application\n\n" +
                "Built with Java 21, JavaFX, and jSMPP\n" +
                "Uses virtual threads for massive concurrency\n\n" +
                "Keyboard Shortcuts:\n" +
                "F5 - Start Test\n" +
                "F6 - Pause/Resume\n" +
                "F7 - Stop Test\n" +
                "Ctrl+O - Load Profile\n" +
                "Ctrl+S - Save Profile"
        );
        alert.showAndWait();
    }

    private TestProfile buildCurrentProfile() {
        TestProfile profile = new TestProfile();
        profile.setName("SMPPulse Profile");
        profile.setConnection(connectionPanelController.buildConfig());
        profile.setLoadTest(loadTestPanelController.buildConfig());
        profile.setDlr(dlrPanelController.buildConfig());
        profile.setMessageTemplate(templatePanelController.buildTemplate());
        return profile;
    }
}
