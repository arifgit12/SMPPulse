package com.smppulse.ui.controller;

import com.smppulse.app.AppContext;
import com.smppulse.metrics.MetricsSnapshot;
import com.smppulse.util.FxUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;

import java.io.File;

public class DashboardController {

    @FXML private Label submittedValue;
    @FXML private Label successValue;
    @FXML private Label failedValue;
    @FXML private Label throttledValue;
    @FXML private Label currentTpsValue;
    @FXML private Label avgTpsValue;
    @FXML private Label peakTpsValue;
    @FXML private Label successRateValue;
    @FXML private Label p50Value;
    @FXML private Label p95Value;
    @FXML private Label p99Value;
    @FXML private Label avgLatencyValue;
    @FXML private Label minLatencyValue;
    @FXML private Label maxLatencyValue;

    @FXML private LineChart<Number, Number> tpsChart;
    @FXML private NumberAxis tpsXAxis;
    @FXML private NumberAxis tpsYAxis;
    @FXML private PieChart resultPieChart;

    private XYChart.Series<Number, Number> tpsSeries;
    private int tpsDataPointIndex = 0;
    private static final int MAX_DATA_POINTS = 120;

    @FXML
    public void initialize() {
        tpsSeries = new XYChart.Series<>();
        tpsSeries.setName("TPS");
        tpsChart.getData().add(tpsSeries);
        tpsChart.setLegendVisible(false);

        resultPieChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Success", 0),
                new PieChart.Data("Failed", 0)
        ));
    }

    public void updateMetrics(MetricsSnapshot snapshot) {
        FxUtils.runOnFxThread(() -> {
            // Counter tiles
            submittedValue.setText(formatNumber(snapshot.submitted()));
            successValue.setText(formatNumber(snapshot.success()));
            failedValue.setText(formatNumber(snapshot.failed()));
            throttledValue.setText(formatNumber(snapshot.throttled()));

            // TPS tiles
            currentTpsValue.setText(String.format("%.0f", snapshot.currentTps()));
            avgTpsValue.setText(String.format("%.1f", snapshot.averageTps()));
            peakTpsValue.setText(String.format("%.0f", snapshot.peakTps()));
            successRateValue.setText(String.format("%.1f%%", snapshot.successRate()));

            // Latency tiles
            p50Value.setText(snapshot.latencyP50() + " ms");
            p95Value.setText(snapshot.latencyP95() + " ms");
            p99Value.setText(snapshot.latencyP99() + " ms");
            avgLatencyValue.setText(String.format("%.1f ms", snapshot.latencyAvg()));
            minLatencyValue.setText(snapshot.latencyMin() + " ms");
            maxLatencyValue.setText(snapshot.latencyMax() + " ms");

            // TPS chart
            tpsSeries.getData().add(new XYChart.Data<>(tpsDataPointIndex++, snapshot.currentTps()));
            if (tpsSeries.getData().size() > MAX_DATA_POINTS) {
                tpsSeries.getData().removeFirst();
            }

            // Update X axis range
            if (tpsDataPointIndex > 60) {
                tpsXAxis.setLowerBound(tpsDataPointIndex - 60);
                tpsXAxis.setUpperBound(tpsDataPointIndex);
            }

            // Pie chart
            ObservableList<PieChart.Data> pieData = resultPieChart.getData();
            if (pieData.size() >= 2) {
                pieData.get(0).setPieValue(snapshot.success());
                pieData.get(1).setPieValue(snapshot.failed());
            }
        });
    }

    public void reset() {
        FxUtils.runOnFxThread(() -> {
            tpsSeries.getData().clear();
            tpsDataPointIndex = 0;
            tpsXAxis.setLowerBound(0);
            tpsXAxis.setUpperBound(60);

            submittedValue.setText("0");
            successValue.setText("0");
            failedValue.setText("0");
            throttledValue.setText("0");
            currentTpsValue.setText("0");
            avgTpsValue.setText("0");
            peakTpsValue.setText("0");
            successRateValue.setText("0%");
            p50Value.setText("0 ms");
            p95Value.setText("0 ms");
            p99Value.setText("0 ms");
            avgLatencyValue.setText("0 ms");
            minLatencyValue.setText("0 ms");
            maxLatencyValue.setText("0 ms");
        });
    }

    @FXML
    private void handleExportCsv() {
        exportWithChooser("Export Results (CSV)", "CSV Files", "*.csv", "results.csv", "csv");
    }

    @FXML
    private void handleExportJson() {
        exportWithChooser("Export Results (JSON)", "JSON Files", "*.json", "results.json", "json");
    }

    @FXML
    private void handleExportHtml() {
        exportWithChooser("Export Results (HTML)", "HTML Files", "*.html", "results.html", "html");
    }

    private void exportWithChooser(String title, String filterDesc, String filterExt, String defaultName, String type) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(filterDesc, filterExt));
        chooser.setInitialFileName(defaultName);
        File file = chooser.showSaveDialog(tpsChart.getScene().getWindow());
        if (file != null) {
            try {
                var exporter = AppContext.getInstance().getResultExporter();
                switch (type) {
                    case "csv" -> exporter.exportCsv(file.toPath());
                    case "json" -> exporter.exportJson(file.toPath());
                    case "html" -> exporter.exportHtml(file.toPath());
                }
                FxUtils.showInfo("Export Complete", "Results exported to: " + file.getName());
            } catch (Exception e) {
                FxUtils.showError("Export Failed", e.getMessage());
            }
        }
    }

    private String formatNumber(long value) {
        if (value >= 1_000_000) return String.format("%.1fM", value / 1_000_000.0);
        if (value >= 1_000) return String.format("%.1fK", value / 1_000.0);
        return String.valueOf(value);
    }
}
