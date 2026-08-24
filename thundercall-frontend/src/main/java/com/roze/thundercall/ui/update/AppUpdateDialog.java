package com.roze.thundercall.ui.update;

import com.roze.thundercall.ui.utils.ThemeManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.nio.file.Path;

/** Professional update dialog for Thundercall. */
public final class AppUpdateDialog {
    private AppUpdateDialog() {}

    public static void check(Window owner, boolean interactive) {
        Thread.startVirtualThread(() -> {
            try {
                AppUpdate update = AppUpdateService.checkForUpdate();
                Platform.runLater(() -> showResult(owner, update, interactive));
            } catch (Exception e) {
                if (interactive) Platform.runLater(() -> showError(owner, "Unable to check for updates", e.getMessage()));
            }
        });
    }

    private static void showResult(Window owner, AppUpdate update, boolean interactive) {
        if (!update.available() || update.preferredUpdateArtifact() == null) {
            if (interactive) showInfo(owner, "You're up to date", "Thundercall " + AppUpdateService.CURRENT_VERSION + " is the latest available version.");
            return;
        }
        AppUpdate.ReleaseArtifact artifact = update.preferredUpdateArtifact();
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Thundercall Update");
        dialog.setHeaderText("Thundercall " + update.latestVersion() + " is available");
        ButtonType later = new ButtonType("Later", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType download = new ButtonType("Download & Install", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(later, download);

        Label current = new Label("Current version: " + AppUpdateService.CURRENT_VERSION);
        Label next = new Label("New version: " + update.latestVersion());
        Label platform = new Label("Platform: " + AppUpdateService.platform() + " • " + AppUpdateService.architecture());
        Label packageInfo = new Label("Package: " + artifact.fileName() + " • " + artifact.packageType().toUpperCase());
        TextArea notes = new TextArea(update.releaseNotes() == null ? "" : update.releaseNotes());
        notes.setEditable(false); notes.setWrapText(true); notes.setPrefRowCount(7);
        ProgressBar progress = new ProgressBar(0); progress.setMaxWidth(Double.MAX_VALUE); progress.setVisible(false);
        Label status = new Label();
        VBox content = new VBox(8, current, next, platform, packageInfo, new Label("Release notes"), notes, progress, status);
        content.setPadding(new Insets(4)); content.setPrefWidth(520);
        dialog.getDialogPane().setContent(content);
        ThemeManager.styleDialog(dialog.getDialogPane());
        Button installButton = (Button) dialog.getDialogPane().lookupButton(download);
        installButton.setDisable(false);
        installButton.setOnAction(e -> {
            installButton.setDisable(true); progress.setVisible(true); status.setText("Downloading and verifying package…");
            Thread.startVirtualThread(() -> {
                try {
                    Path file = AppUpdateService.downloadAndVerify(artifact, (done,total) -> Platform.runLater(() -> progress.setProgress(total > 0 ? (double) done / total : -1)));
                    Platform.runLater(() -> {
                        status.setText("Verified. Preparing installer…");
                        try { AppUpdateService.installAndRestart(file, artifact); } catch (Exception ex) { installButton.setDisable(false); progress.setVisible(false); showError(owner, "Installation failed", ex.getMessage()); }
                    });
                } catch (Exception ex) { Platform.runLater(() -> { installButton.setDisable(false); progress.setVisible(false); status.setText("Download failed"); showError(owner, "Update failed", ex.getMessage()); }); }
            });
        });
        if (update.mandatory()) {
            Button laterButton = (Button) dialog.getDialogPane().lookupButton(later);

            if (laterButton != null) {
                laterButton.setDisable(true);
            }

            // Prevent closing the mandatory update dialog
            dialog.setOnCloseRequest(e -> e.consume());
        }

        dialog.showAndWait();
    }

    private static void showInfo(Window owner, String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.initOwner(owner); a.setTitle(title); a.setHeaderText(null); a.setContentText(message); ThemeManager.styleDialog(a.getDialogPane()); a.showAndWait();
    }
    private static void showError(Window owner, String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.initOwner(owner); a.setTitle(title); a.setHeaderText(null); a.setContentText(message == null ? "Unknown error" : message); ThemeManager.styleDialog(a.getDialogPane()); a.showAndWait();
    }
}
