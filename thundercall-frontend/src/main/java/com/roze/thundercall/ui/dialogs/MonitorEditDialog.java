package com.roze.thundercall.ui.dialogs;

import com.roze.thundercall.ui.models.CollectionResponse;
import com.roze.thundercall.ui.models.EnvironmentResponse;
import com.roze.thundercall.ui.models.MonitorRequest;
import com.roze.thundercall.ui.models.MonitorResponse;
import com.roze.thundercall.ui.services.CollectionService;
import com.roze.thundercall.ui.services.EnvironmentService;
import com.roze.thundercall.ui.services.MonitorService;
import com.roze.thundercall.ui.utils.AlertUtils;
import com.roze.thundercall.ui.utils.ThemeManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * Modern, interactive Postman-grade dialog for creating and configuring
 * automated Collection Monitors.
 */
public class MonitorEditDialog {

    private static final Map<String, Integer> INTERVAL_PRESETS = new LinkedHashMap<>();

    static {
        INTERVAL_PRESETS.put("Every 5 minutes", 5);
        INTERVAL_PRESETS.put("Every 15 minutes", 15);
        INTERVAL_PRESETS.put("Every 30 minutes", 30);
        INTERVAL_PRESETS.put("Every hour", 60);
        INTERVAL_PRESETS.put("Every 6 hours", 360);
        INTERVAL_PRESETS.put("Every 12 hours", 720);
        INTERVAL_PRESETS.put("Every day", 1440);
    }

    public static void show(MonitorResponse existing, Window ownerWindow, Consumer<MonitorResponse> onSaved) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Create Monitor" : "Configure Monitor — " + existing.getName());
        dialog.setResizable(true);
        if (ownerWindow != null) {
            dialog.initOwner(ownerWindow);
        }

        DialogPane pane = dialog.getDialogPane();
        ButtonType saveBtnType = new ButtonType(existing == null ? "Create Monitor" : "Save Changes", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(ButtonType.CANCEL, saveBtnType);
        ThemeManager.styleDialog(pane);

        dialog.setWidth(720);
        dialog.setHeight(700);
        pane.setPrefSize(720, 700);
        pane.setMinWidth(640);
        pane.setMinHeight(560);

        dialog.setOnShowing(e -> {
            Window win = pane.getScene() != null ? pane.getScene().getWindow() : null;
            if (win instanceof javafx.stage.Stage stage) {
                stage.setWidth(720);
                stage.setHeight(700);
                stage.setMinWidth(640);
                stage.setMinHeight(560);
                stage.centerOnScreen();
            }
        });

        // Header
        Label iconLabel = new Label("⏱");
        iconLabel.setStyle("-fx-font-size: 26px; -fx-text-fill: -accent;");

        Label titleLabel = new Label(existing == null ? "Create a Monitor" : "Configure Monitor");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -text-primary;");

        Label descLabel = new Label("Regularly run a collection to check performance, response status, and availability.");
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-muted;");
        descLabel.setWrapText(true);

        VBox titleBox = new VBox(2, titleLabel, descLabel);
        HBox header = new HBox(12, iconLabel, titleBox);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(4, 0, 8, 0));

        // Form Fields
        // 1. Monitor Name
        Label nameHeading = new Label("Monitor Name *");
        nameHeading.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: -text-primary;");

        TextField nameField = new TextField(existing != null ? existing.getName() : "");
        nameField.setPromptText("e.g. Daily Production Health Check or Staging Auth API");

        // 2. Collection & Environment Row
        Label colHeading = new Label("Collection *");
        colHeading.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: -text-primary;");

        ComboBox<String> collectionCombo = new ComboBox<>();
        collectionCombo.setPromptText("Loading collections...");
        collectionCombo.setMaxWidth(Double.MAX_VALUE);
        Map<String, Long> collectionIds = new LinkedHashMap<>();

        Label envHeading = new Label("Environment (Optional)");
        envHeading.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: -text-primary;");

        ComboBox<String> environmentCombo = new ComboBox<>();
        environmentCombo.getItems().add("(no environment)");
        environmentCombo.setValue("(no environment)");
        environmentCombo.setMaxWidth(Double.MAX_VALUE);
        Map<String, Long> environmentIds = new LinkedHashMap<>();

        Label envHint = new Label("Environment variables such as {{baseUrl}} and {{token}} are resolved at run time.");
        envHint.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");

        VBox colBox = new VBox(4, colHeading, collectionCombo);
        HBox.setHgrow(colBox, Priority.ALWAYS);
        VBox envBox = new VBox(4, envHeading, environmentCombo);
        HBox.setHgrow(envBox, Priority.ALWAYS);

        HBox colEnvRow = new HBox(14, colBox, envBox);

        // Card 1: Target Collection Card
        VBox targetCard = new VBox(10, nameHeading, nameField, colEnvRow, envHint);
        targetCard.getStyleClass().add("monitor-kpi-card");

        // Card 2: Schedule & Cadence
        Label scheduleHeading = new Label("Run Frequency & Cadence");
        scheduleHeading.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: -text-primary;");

        ComboBox<String> intervalCombo = new ComboBox<>();
        intervalCombo.getItems().addAll(INTERVAL_PRESETS.keySet());
        intervalCombo.setValue("Every 5 minutes");
        intervalCombo.setMaxWidth(Double.MAX_VALUE);

        if (existing != null) {
            for (Map.Entry<String, Integer> entry : INTERVAL_PRESETS.entrySet()) {
                if (entry.getValue() == existing.getIntervalMinutes()) {
                    intervalCombo.setValue(entry.getKey());
                    break;
                }
            }
        }

        // Quick Preset Chips
        FlowPane presetPills = new FlowPane();
        presetPills.setHgap(6);
        presetPills.setVgap(6);

        Map<String, String> pillLabels = new LinkedHashMap<>();
        pillLabels.put("5m", "Every 5 minutes");
        pillLabels.put("15m", "Every 15 minutes");
        pillLabels.put("30m", "Every 30 minutes");
        pillLabels.put("1h", "Every hour");
        pillLabels.put("6h", "Every 6 hours");
        pillLabels.put("12h", "Every 12 hours");
        pillLabels.put("Daily", "Every day");

        List<Button> pillButtons = new ArrayList<>();
        for (Map.Entry<String, String> entry : pillLabels.entrySet()) {
            Button pill = new Button(entry.getKey());
            pill.getStyleClass().add("preset-chip-btn");
            pill.setOnAction(e -> intervalCombo.setValue(entry.getValue()));
            pillButtons.add(pill);
            presetPills.getChildren().add(pill);
        }

        Label cadenceHint = new Label("Thundercall runs all requests in the collection sequentially according to this timer.");
        cadenceHint.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");

        VBox scheduleCard = new VBox(10, scheduleHeading, intervalCombo, presetPills, cadenceHint);
        scheduleCard.getStyleClass().add("monitor-kpi-card");

        // Card 3: Notifications & Activation
        Label alertHeading = new Label("Notifications & Activation");
        alertHeading.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: -text-primary;");

        CheckBox notifyCheck = new CheckBox("Email notifications for run failures");
        notifyCheck.setSelected(existing == null || existing.isNotifyOnFailure());
        notifyCheck.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-primary;");

        TextField notifyEmailField = new TextField(existing != null ? existing.getNotifyEmail() : "");
        notifyEmailField.setPromptText("Recipient email (defaults to your account email if empty)");
        notifyEmailField.disableProperty().bind(notifyCheck.selectedProperty().not());

        CheckBox enabledCheck = new CheckBox("Enable monitor immediately");
        enabledCheck.setSelected(existing == null || existing.isEnabled());
        enabledCheck.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-primary;");

        VBox alertCard = new VBox(10, alertHeading, notifyCheck, notifyEmailField, enabledCheck);
        alertCard.getStyleClass().add("monitor-kpi-card");

        // Status Feedback Label
        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -danger;");
        statusLabel.setWrapText(true);

        VBox formContainer = new VBox(14, header, targetCard, scheduleCard, alertCard, statusLabel);
        formContainer.setPadding(new Insets(16));
        formContainer.setMinWidth(640);
        formContainer.setPrefWidth(680);

        ScrollPane scrollPane = new ScrollPane(formContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportWidth(680);
        scrollPane.setPrefViewportHeight(580);
        scrollPane.setMinWidth(640);
        scrollPane.setMinHeight(500);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        pane.setContent(scrollPane);

        dialog.setOnShown(e -> {
            Window win = pane.getScene() != null ? pane.getScene().getWindow() : null;
            if (win instanceof javafx.stage.Stage stage) {
                stage.setWidth(720);
                stage.setHeight(700);
                stage.setMinWidth(640);
                stage.setMinHeight(560);
                stage.centerOnScreen();
            }
            Platform.runLater(nameField::requestFocus);
        });

        // Fetch collections and environments asynchronously
        new Thread(() -> {
            Optional<List<CollectionResponse>> collections = CollectionService.getUserCollections();
            Optional<List<EnvironmentResponse>> environments = EnvironmentService.getUserEnvironments();
            Platform.runLater(() -> {
                collectionCombo.setPromptText("Select a collection to monitor");
                collections.ifPresent(list -> {
                    for (CollectionResponse c : list) {
                        String display = c.getName() + " (" + c.getRequestCount() + " requests)";
                        collectionIds.put(display, c.getId());
                        collectionCombo.getItems().add(display);
                    }
                    if (existing != null) {
                        collectionIds.entrySet().stream()
                                .filter(en -> en.getValue().equals(existing.getCollectionId()))
                                .findFirst()
                                .ifPresent(en -> collectionCombo.setValue(en.getKey()));
                    }
                });

                environments.ifPresent(list -> {
                    for (EnvironmentResponse env : list) {
                        environmentIds.put(env.getName(), env.getId());
                        environmentCombo.getItems().add(env.getName());
                    }
                    if (existing != null && existing.getEnvironmentId() != null) {
                        environmentIds.entrySet().stream()
                                .filter(en -> en.getValue().equals(existing.getEnvironmentId()))
                                .findFirst()
                                .ifPresent(en -> environmentCombo.setValue(en.getKey()));
                    }
                });
            });
        }).start();

        // Save logic
        Button saveBtn = (Button) pane.lookupButton(saveBtnType);
        saveBtn.getStyleClass().add("primary-btn");
        saveBtn.addEventFilter(ActionEvent.ACTION, event -> {
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            Long colId = collectionIds.get(collectionCombo.getValue());

            if (name.isEmpty()) {
                statusLabel.setText("Please specify a monitor name.");
                nameField.requestFocus();
                event.consume();
                return;
            }

            if (colId == null) {
                statusLabel.setText("Please select a target collection to run.");
                collectionCombo.requestFocus();
                event.consume();
                return;
            }

            Long envId = environmentIds.get(environmentCombo.getValue());
            int intervalMinutes = INTERVAL_PRESETS.getOrDefault(intervalCombo.getValue(), 5);

            MonitorRequest req = new MonitorRequest(
                    name, colId, envId, intervalMinutes,
                    enabledCheck.isSelected(), notifyCheck.isSelected(), notifyEmailField.getText()
            );

            saveBtn.setDisable(true);
            saveBtn.setText("Saving...");
            statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-muted;");
            statusLabel.setText("Saving monitor configuration...");

            new Thread(() -> {
                try {
                    MonitorResponse saved;
                    if (existing == null) {
                        saved = MonitorService.createMonitor(req);
                    } else {
                        saved = MonitorService.updateMonitor(existing.getId(), req);
                    }

                    Platform.runLater(() -> {
                        if (onSaved != null) {
                            onSaved.accept(saved);
                        }
                        dialog.setResult(saveBtnType);
                        dialog.close();
                    });
                } catch (IOException ex) {
                    Platform.runLater(() -> {
                        saveBtn.setDisable(false);
                        saveBtn.setText(existing == null ? "Create Monitor" : "Save Changes");
                        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -danger;");
                        statusLabel.setText("Error: " + MonitorService.friendlyMessage(ex));
                    });
                }
            }).start();

            event.consume(); // keep dialog open while saving thread runs
        });

        dialog.showAndWait();
    }
}
