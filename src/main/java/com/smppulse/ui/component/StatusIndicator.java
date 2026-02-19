package com.smppulse.ui.component;

import com.smppulse.core.session.SessionEventListener.ConnectionState;
import javafx.scene.layout.Region;

public class StatusIndicator extends Region {

    public StatusIndicator() {
        getStyleClass().add("status-indicator");
        getStyleClass().add("disconnected");
        setPrefSize(12, 12);
        setMinSize(12, 12);
        setMaxSize(12, 12);
    }

    public void setState(ConnectionState state) {
        getStyleClass().removeAll("disconnected", "connecting", "connected", "error");
        switch (state) {
            case DISCONNECTED -> getStyleClass().add("disconnected");
            case CONNECTING, RECONNECTING -> getStyleClass().add("connecting");
            case CONNECTED -> getStyleClass().add("connected");
            case ERROR -> getStyleClass().add("error");
        }
    }
}
