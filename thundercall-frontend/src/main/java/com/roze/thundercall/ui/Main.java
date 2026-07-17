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
        // FIX: this used to clear leftover Stage.setMinWidth/setMinHeight
        // constraints from the main view first — but the main view no
        // longer calls those APIs at all (they're what triggers the GTK
        // resize assertion on this platform; see showMainView()), so
        // there's nothing left to clear, and one less call to that API
        // is one less chance to hit the same crash.
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
            // FIX: deferring the Stage.setMinWidth/setMinHeight calls to
            // the next pulse (previous attempt) still hit the same
            // "gtk_window_resize: assertion 'height > 0' failed" crash —
            // meaning it's not a timing/ordering issue at all, it's that
            // Stage-level min-size enforcement itself is broken on this
            // GTK setup. Constraining the ROOT NODE's own min size
            // instead achieves the identical "can't shrink the icon rail
            // into an unreadable mess" protection through ordinary
            // JavaFX layout — a stage can't be resized smaller than its
            // scene's root minimum size — without ever calling the
            // native Stage min-size API that's crashing.
            if (root instanceof javafx.scene.layout.Region region) {
                region.setMinWidth(860);
                region.setMinHeight(560);
            }
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