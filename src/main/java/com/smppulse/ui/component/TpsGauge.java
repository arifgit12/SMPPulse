package com.smppulse.ui.component;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

public class TpsGauge extends VBox {

    private final Label valueLabel;
    private final ProgressBar progressBar;
    private double maxTps = 100;

    public TpsGauge() {
        getStyleClass().add("counter-tile");
        setAlignment(Pos.CENTER);
        setSpacing(6);

        valueLabel = new Label("0 TPS");
        valueLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #7c3aed;");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(150);

        Label nameLabel = new Label("Current TPS");
        nameLabel.getStyleClass().add("name-label");

        getChildren().addAll(valueLabel, progressBar, nameLabel);
    }

    public void setTps(double tps) {
        valueLabel.setText(String.format("%.0f TPS", tps));
        progressBar.setProgress(Math.min(tps / maxTps, 1.0));
    }

    public void setMaxTps(double maxTps) {
        this.maxTps = maxTps;
    }
}
