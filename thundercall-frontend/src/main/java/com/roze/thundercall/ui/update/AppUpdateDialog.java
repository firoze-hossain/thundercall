package com.roze.thundercall.ui.update;

import com.roze.thundercall.ui.utils.ThemeManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Modern, professional update dialog for Thundercall.
 * Shows version comparisons, release notes, progress bar, and handles
 * one-click download, verification, installation, and clean app restart.
 */
public final class AppUpdateDialog extends Stage {

    private final AppUpdate update;
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label statusLabel = new Label();
    private final Button actionBtn = new Button("Download & Install");
    private final Button closeBtn = new Button("Remind Me Later");
    private Path downloadedFile;

    public static void check(Window owner, boolean interactive) {
        AppUpdateService.checkForUpdateAsync().whenComplete((update, error) -> Platform.runLater(() -> {
            if (error != null) {
                if (interactive) {
                    showError(owner, "Update Check Failed", rootMessage(error));
                }
            } else if (update == null || !update.available()) {
                if (interactive) {
                    showInfo(owner, "You're up to date",
                            "ThunderCall " + AppUpdateService.currentVersion() + " is the latest available version.");
                }
            } else {
                show(owner, update);
            }
        }));
    }

    public static void show(Window owner, AppUpdate update) {
        if (update == null || !update.available()) return;
        Platform.runLater(() -> {
            AppUpdateDialog dialog = new AppUpdateDialog(owner, update);
            dialog.show();
        });
    }

    public AppUpdateDialog(Window owner, AppUpdate update) {
        this.update = update;
        if (owner != null) {
            initOwner(owner);
        }
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);
        setTitle("ThunderCall Update");

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));
        root.setPrefWidth(560);
        root.getStyleClass().add("update-dialog-root");

        // Header Title
        Label title = new Label("A new version of ThunderCall is available");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -fx-text-base-color;");

        // Version comparison & source
        String sourceLabel = "github".equalsIgnoreCase(update.source) ? "GitHub Releases" : "RozeHub Website";
        String versionText = String.format("Current: %s  ➔  Latest: %s  •  Source: %s",
                update.currentVersion(), update.latestVersion(), sourceLabel);
        Label versionLabel = new Label(versionText);
        versionLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #3574F0;");

        // Release notes box
        Label notesHeader = new Label("Release Notes:");
        notesHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-opacity: 0.8;");

        TextArea notesArea = new TextArea(update.releaseNotes());
        notesArea.setEditable(false);
        notesArea.setWrapText(true);
        notesArea.setPrefRowCount(7);
        notesArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        VBox.setVgrow(notesArea, Priority.ALWAYS);

        // Package metadata
        String packageInfo = "";
        AppUpdate.Release release = update.release;
        String fileName = release != null ? release.effectiveFileName() : "";
        long fileSize = release != null ? release.effectiveFileSize() : 0;
        if (fileName != null && !fileName.isBlank()) {
            if (fileSize > 0) {
                double mb = fileSize / (1024.0 * 1024.0);
                packageInfo = String.format(Locale.US, "Platform: %s • %s  |  Package: %s (%.1f MB)",
                        AppUpdateService.platform(), AppUpdateService.architecture(), fileName, mb);
            } else {
                packageInfo = String.format("Platform: %s • %s  |  Package: %s",
                        AppUpdateService.platform(), AppUpdateService.architecture(), fileName);
            }
        }
        Label packageLabel = new Label(packageInfo);
        packageLabel.setStyle("-fx-font-size: 11px; -fx-opacity: 0.75;");

        progressBar.setVisible(false);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: #3574F0;");

        statusLabel.setStyle("-fx-font-size: 12px; -fx-opacity: 0.85;");

        // Action Buttons
        closeBtn.setOnAction(e -> close());

        actionBtn.setStyle("""
            -fx-background-color: #3574F0;
            -fx-text-fill: #FFFFFF;
            -fx-font-weight: bold;
            -fx-border-radius: 4;
            -fx-background-radius: 4;
            -fx-padding: 6 18;
            -fx-cursor: hand;
        """);

        String downloadUrl = release != null ? release.effectiveDownloadUrl() : null;
        if (downloadUrl == null || downloadUrl.isBlank()) {
            actionBtn.setDisable(true);
            statusLabel.setText("No download package attached to this release.");
        }

        actionBtn.setOnAction(e -> {
            if (downloadedFile != null) {
                try {
                    statusLabel.setText("Launching installer…");
                    AppUpdateService.installAndRestart(downloadedFile);
                } catch (Exception ex) {
                    statusLabel.setStyle("-fx-text-fill: #E53935; -fx-font-weight: bold;");
                    statusLabel.setText("Failed to start installer: " + ex.getMessage());
                }
            } else {
                startDownload();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox btnBox = new HBox(10, closeBtn, spacer, actionBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(title, versionLabel, notesHeader, notesArea, packageLabel, progressBar, statusLabel, btnBox);

        Scene scene = new Scene(root);
        try {
            String mainCss = getClass().getResource("/css/main.css") != null
                    ? getClass().getResource("/css/main.css").toExternalForm() : null;
            if (mainCss != null) scene.getStylesheets().add(mainCss);
            ThemeManager.apply(scene);
        } catch (Exception ignored) {}

        setScene(scene);

        if (update.mandatory()) {
            closeBtn.setDisable(true);
            setOnCloseRequest(e -> e.consume());
        }
    }

    private void startDownload() {
        actionBtn.setDisable(true);
        if (!update.mandatory()) {
            closeBtn.setDisable(true);
        }
        progressBar.setVisible(true);
        progressBar.setProgress(0);
        statusLabel.setStyle("-fx-text-fill: #3574F0;");
        statusLabel.setText("Downloading installer package…");

        AppUpdate.Release release = update.release;
        if (release == null) {
            statusLabel.setText("Release details unavailable.");
            return;
        }

        AppUpdateService.download(release, p -> Platform.runLater(() -> {
            progressBar.setProgress(p);
            statusLabel.setText(String.format(Locale.US, "Downloading %s… (%.0f%%)",
                    release.effectiveFileName(), p * 100));
        })).whenComplete((file, err) -> Platform.runLater(() -> {
            closeBtn.setDisable(false);
            if (err != null) {
                statusLabel.setStyle("-fx-text-fill: #E53935; -fx-font-weight: bold;");
                statusLabel.setText("Download failed: " + rootMessage(err));
                actionBtn.setText("Retry Download");
                actionBtn.setDisable(false);
            } else {
                downloadedFile = file;
                progressBar.setProgress(1.0);
                statusLabel.setStyle("-fx-text-fill: #2EA043; -fx-font-weight: bold;");
                statusLabel.setText("Verified package ready: " + file.getFileName());

                actionBtn.setText("Restart & Install Now");
                actionBtn.setDisable(false);
                actionBtn.setStyle("""
                    -fx-background-color: #2EA043;
                    -fx-text-fill: #FFFFFF;
                    -fx-font-weight: bold;
                    -fx-border-radius: 4;
                    -fx-background-radius: 4;
                    -fx-padding: 6 18;
                    -fx-cursor: hand;
                """);
                closeBtn.setText("Install on Exit");
            }
        }));
    }

    private static void showInfo(Window owner, String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        if (owner != null) a.initOwner(owner);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        ThemeManager.styleDialog(a.getDialogPane());
        a.showAndWait();
    }

    private static void showError(Window owner, String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        if (owner != null) a.initOwner(owner);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message == null ? "Unknown error" : message);
        ThemeManager.styleDialog(a.getDialogPane());
        a.showAndWait();
    }

    private static String rootMessage(Throwable error) {
        Throwable t = error;
        while (t.getCause() != null && (t.getMessage() == null || t.getMessage().isBlank())) {
            t = t.getCause();
        }
        return t.getMessage() == null ? t.toString() : t.getMessage();
    }
}
