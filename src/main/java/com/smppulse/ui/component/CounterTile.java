package com.smppulse.ui.component;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class CounterTile extends VBox {

    private final Label valueLabel;
    private final Label nameLabel;

    public CounterTile(String name, String color) {
        getStyleClass().add("counter-tile");
        setAlignment(Pos.CENTER);
        setSpacing(4);

        valueLabel = new Label("0");
        valueLabel.getStyleClass().add("value-label");
        if (color != null) {
            valueLabel.setStyle("-fx-text-fill: " + color + ";");
        }

        nameLabel = new Label(name);
        nameLabel.getStyleClass().add("name-label");

        getChildren().addAll(valueLabel, nameLabel);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public void setValue(long value) {
        if (value >= 1_000_000) {
            valueLabel.setText(String.format("%.1fM", value / 1_000_000.0));
        } else if (value >= 1_000) {
            valueLabel.setText(String.format("%.1fK", value / 1_000.0));
        } else {
            valueLabel.setText(String.valueOf(value));
        }
    }
}
