package com.smppulse.ui.component;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class LatencyBar extends VBox {

    private final Label p50Label;
    private final Label p95Label;
    private final Label p99Label;
    private final ProgressBar p50Bar;
    private final ProgressBar p95Bar;
    private final ProgressBar p99Bar;
    private double maxLatency = 1000;

    public LatencyBar() {
        setSpacing(6);

        p50Label = createLabel("P50: 0 ms");
        p95Label = createLabel("P95: 0 ms");
        p99Label = createLabel("P99: 0 ms");

        p50Bar = createBar("#059669");
        p95Bar = createBar("#d97706");
        p99Bar = createBar("#dc2626");

        getChildren().addAll(
                createRow("P50", p50Label, p50Bar),
                createRow("P95", p95Label, p95Bar),
                createRow("P99", p99Label, p99Bar)
        );
    }

    public void update(long p50, long p95, long p99) {
        long max = Math.max(p99, 1);
        maxLatency = max * 1.2;

        p50Label.setText(p50 + " ms");
        p95Label.setText(p95 + " ms");
        p99Label.setText(p99 + " ms");

        p50Bar.setProgress(p50 / maxLatency);
        p95Bar.setProgress(p95 / maxLatency);
        p99Bar.setProgress(p99 / maxLatency);
    }

    private HBox createRow(String name, Label valueLabel, ProgressBar bar) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(name);
        nameLabel.setMinWidth(30);
        nameLabel.setStyle("-fx-text-fill: #888898; -fx-font-size: 11px;");
        bar.setPrefWidth(120);
        row.getChildren().addAll(nameLabel, bar, valueLabel);
        return row;
    }

    private Label createLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-family: monospace; -fx-font-size: 12px; -fx-text-fill: #e0e0e0;");
        return label;
    }

    private ProgressBar createBar(String color) {
        ProgressBar bar = new ProgressBar(0);
        bar.setStyle("-fx-accent: " + color + ";");
        return bar;
    }
}
