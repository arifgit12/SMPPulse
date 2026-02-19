package com.smppulse.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmppPulseApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(SmppPulseApp.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        log.info("Starting SMPPulse application");

        AppContext.initialize();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/MainView.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());

        primaryStage.setTitle("SMPPulse - SMPP Load Testing");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(700);

        primaryStage.setOnCloseRequest(event -> {
            log.info("Shutting down SMPPulse");
            AppContext.getInstance().shutdown();
        });

        primaryStage.show();
        log.info("SMPPulse started successfully");
    }
}
