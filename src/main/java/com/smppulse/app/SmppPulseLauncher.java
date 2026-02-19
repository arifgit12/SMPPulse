package com.smppulse.app;

import javafx.application.Application;

/**
 * Non-Application main class for shade JAR compatibility.
 * JavaFX Application subclasses cannot be the main class in a fat JAR.
 */
public class SmppPulseLauncher {

    public static void main(String[] args) {
        Application.launch(SmppPulseApp.class, args);
    }
}
