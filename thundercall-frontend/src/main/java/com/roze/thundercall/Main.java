package com.roze.thundercall;

import com.roze.thundercall.utils.AlertUtils;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class Main extends Application {
    private static Stage primaryStage;
    private static HostServices hostServices;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        hostServices = getHostServices();
        showLoginView();
    }

    public static void showLoginView() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/views/login.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Login");
            primaryStage.show();
        } catch (IOException e) {
            AlertUtils.showError("Failed to load login view");
        }
    }

    public static void showRegisterView() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/views/register.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 880, 600);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Register");
            primaryStage.show();
        } catch (IOException e) {
            AlertUtils.showError("Failed to load register view");
        }
    }

    public static void showMainView() {
        try {
            System.out.println("Attempting to load main.fxml...");

            // Check if the FXML file exists
            URL fxmlUrl = Main.class.getResource("/views/main.fxml");
            if (fxmlUrl == null) {
                throw new IOException("main.fxml not found at /views/main.fxml");
            }
            System.out.println("FXML URL: " + fxmlUrl);

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            System.out.println("FXML loaded successfully");

            Scene scene = new Scene(root, 1200, 800);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Thundercall Client");
            primaryStage.show();

            System.out.println("Main view displayed successfully");

        } catch (Exception e) {
            System.err.println("ERROR loading main view: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Failed to load main view: " + e.getMessage());
        }
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static HostServices getAppHostServices() {
        return hostServices;
    }

    public static void main(String[] args) {
        launch(args);
    }
}