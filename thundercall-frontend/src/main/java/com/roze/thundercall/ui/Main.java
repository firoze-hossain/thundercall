package com.roze.thundercall.ui;

import com.roze.thundercall.ui.controllers.AuthController;
import com.roze.thundercall.ui.services.ApiClient;
import com.roze.thundercall.ui.services.TokenManager;
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

        // If an API call discovers the 7-day session has fully expired,
        // return to the login screen with a clear explanation.
        ApiClient.setOnSessionExpired(() -> {
            com.roze.thundercall.ui.services.WorkspaceManager.clear();
            AlertUtils.showInfo("Your 7-day session has expired — please log in again.");
            showLoginView();
        });

        // Check if an unexpired 7-day session exists
        if (TokenManager.restoreSession()) {
            System.out.println("[Main] Restored active 7-day session for " + TokenManager.getUsername()
                    + " (" + TokenManager.getSessionTimeRemainingHumanReadable() + ")");
            showMainView();
        } else {
            showLoginView();
            if (TokenManager.wasSessionExpired()) {
                TokenManager.clearSessionExpiredAlert();
                AlertUtils.showInfo("Your 7-day session has expired. Please log in again.");
            }
        }
    }

    public static void showLoginView() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/views/login.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 880, 600);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Thundercall — Sign In");
            resetStageToSceneSize(880, 600);
            primaryStage.show();
        } catch (IOException e) {
            AlertUtils.showError("Failed to load login view");
        }
    }

    public static void showRegisterView() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/views/register.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 880, 600);
            AuthController controller = fxmlLoader.getController();
            if (controller != null) {
                controller.setRegisterMode(true);
            }
            primaryStage.setScene(scene);
            primaryStage.setTitle("Thundercall — Create Account");
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

    /**
     * Resets stage size and maximized state when switching to an auth view.
     */
    private static void resetStageToSceneSize(double width, double height) {
        if (primaryStage == null) {
            return;
        }

        if (primaryStage.isMaximized()) {
            javafx.beans.value.ChangeListener<Boolean> maxListener = new javafx.beans.value.ChangeListener<>() {
                @Override
                public void changed(javafx.beans.value.ObservableValue<? extends Boolean> obs, Boolean oldVal, Boolean newVal) {
                    if (!newVal) {
                        primaryStage.maximizedProperty().removeListener(this);
                        primaryStage.setWidth(width);
                        primaryStage.setHeight(height);
                        primaryStage.centerOnScreen();
                    }
                }
            };
            primaryStage.maximizedProperty().addListener(maxListener);
            primaryStage.setMaximized(false);
        }

        primaryStage.setWidth(width);
        primaryStage.setHeight(height);
        primaryStage.centerOnScreen();

        // On Linux / GTK, window manager unmaximize event is asynchronous.
        // A brief delayed pass guarantees that even if X11 took time to finish
        // the unmaximize transition, the stage bounds and center position are applied.
        javafx.animation.PauseTransition pulse = new javafx.animation.PauseTransition(javafx.util.Duration.millis(80));
        pulse.setOnFinished(e -> {
            if (primaryStage.isMaximized()) {
                primaryStage.setMaximized(false);
            }
            primaryStage.setWidth(width);
            primaryStage.setHeight(height);
            primaryStage.centerOnScreen();
        });
        pulse.play();
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