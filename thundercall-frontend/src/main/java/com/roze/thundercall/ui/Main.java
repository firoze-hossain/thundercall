package com.roze.thundercall.ui;

import com.roze.thundercall.ui.utils.AlertUtils;
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
        // If a background call ever discovers the session has fully
        // expired (refresh token expired too), return to the login screen
        // with a clear explanation instead of leaving the app stuck.
        com.roze.thundercall.ui.services.ApiClient.setOnSessionExpired(() -> {
            AlertUtils.showInfo("Your session has expired — please log in again.");
            showLoginView();
        });
        showLoginView();
    }

    public static void showLoginView() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/views/login.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Login");
            resetStageToSceneSize(800, 600);
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
            resetStageToSceneSize(880, 600);
            primaryStage.show();
        } catch (IOException e) {
            AlertUtils.showError("Failed to load register view");
        }
    }

    public static void showVerifyEmailView(String email) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/views/verify-email.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 880, 600);
            com.roze.thundercall.ui.controllers.VerifyEmailController controller = fxmlLoader.getController();
            controller.setEmail(email);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Verify your email");
            resetStageToSceneSize(880, 600);
            primaryStage.show();
        } catch (IOException e) {
            AlertUtils.showError("Failed to load the verification view");
        }
    }

    /** FIX: setScene() alone never resets the STAGE's own width/height or
     * maximized state — only the Scene's preferred size. If the main
     * window had been resized or maximized, logging out would swap in
     * the (smaller, fixed-layout) login scene but leave the stage at
     * whatever large size it already was, stretching the login card
     * across it. Auth views always get their own correct size now,
     * regardless of what size the previous view left the window at. */
    private static void resetStageToSceneSize(double width, double height) {
        // Clear the main view's minimum constraints first — otherwise a
        // stage left at 860x560 minimum (set for the main view) would
        // clamp this smaller auth window instead of actually shrinking it.
        primaryStage.setMinWidth(0);
        primaryStage.setMinHeight(0);
        primaryStage.setMaximized(false);
        primaryStage.setWidth(width);
        primaryStage.setHeight(height);
        primaryStage.centerOnScreen();
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
            // FIX: nothing stopped the window being resized down to a
            // width where the icon rail's labels ("Collections",
            // "Environments", "History") get clipped mid-word and the
            // sidebar/request panes overlap. A sane minimum keeps every
            // panel legible; the window can still be resized freely
            // above this, and the sidebar/response dividers still work
            // exactly as before.
            primaryStage.setMinWidth(860);
            primaryStage.setMinHeight(560);
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