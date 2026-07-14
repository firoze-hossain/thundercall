package com.roze.thundercall.ui.utils;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/**
 * FIX: Alerts open in their own window and do not inherit the app
 * stylesheet — they used to appear as bright default dialogs on top of the
 * dark app. Every alert is now styled through ThemeManager so popups match
 * the current theme (dark or light).
 */
public class AlertUtils {
    /** When true, showError/showInfo log to console instead of popping a
     * modal dialog — used during bulk imports so one bad item doesn't
     * force the person to click through a dialog for every failure. */
    private static volatile boolean quiet = false;

    /** Bulk operations (import, collection runner, etc.) turn this on for
     * their duration and back off in a finally block. */
    public static void setQuiet(boolean value) {
        quiet = value;
    }

    public static void showError(String message) {
        if (quiet) {
            System.out.println("[Suppressed error during bulk operation] " + message);
            return;
        }
        showAlert(Alert.AlertType.ERROR, "Error", message);
    }

    public static void showSuccess(String message) {
        showAlert(Alert.AlertType.INFORMATION, "Success", message);
    }

    private static void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setContentText(message);
            alert.setHeaderText(null);
            ThemeManager.styleDialog(alert.getDialogPane());
            alert.showAndWait();
        });
    }

    public static void showInfo(String message) {
        showAlert(Alert.AlertType.INFORMATION, "Information", message);
    }

    public static boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        ThemeManager.styleDialog(alert.getDialogPane());
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}