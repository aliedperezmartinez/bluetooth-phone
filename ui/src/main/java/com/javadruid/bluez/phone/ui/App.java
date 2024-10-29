package com.javadruid.bluez.phone.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private PrimaryController controller;

    @Override
    public void start(Stage stage) throws IOException {
        final FXMLLoader loader = new FXMLLoader(getClass().getResource( "primary.fxml"));
        final Scene scene = new Scene(loader.load());
        controller = loader.getController();
        scene.setOnKeyPressed(controller::enterKey);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setTitle("Bluetooth dialer");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        controller.close();
    }

}