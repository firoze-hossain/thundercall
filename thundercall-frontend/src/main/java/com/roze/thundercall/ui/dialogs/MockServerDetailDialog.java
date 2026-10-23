package com.roze.thundercall.ui.dialogs;

import com.roze.thundercall.ui.models.*;
import com.roze.thundercall.ui.services.CollectionService;
import com.roze.thundercall.ui.services.MockServerService;
import com.roze.thundercall.ui.utils.AlertUtils;
import com.roze.thundercall.ui.utils.ThemeManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.Window;
import javafx.util.Duration;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MockServerDetailDialog {

    public static void show(MockServerResponse server,
                            Window ownerWindow,
                            Consumer<MockServerResponse> onUpdated,
                            BiConsumer<String, String> onTestInTab) {

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Mock Server — " + server.getName());
        dialog.setResizable(true);
        if (ownerWindow != null) {
            dialog.initOwner(ownerWindow);
        }

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().add(ButtonType.CLOSE);
        ThemeManager.styleDialog(pane);
        pane.setPrefSize(1020, 720);
        pane.setMinWidth(860);
        pane.setMinHeight(600);

        // State holder
        final boolean[] isEnabled = {server.isEnabled()};
        final ObservableList<MockRouteResponse> routesData = FXCollections.observableArrayList();
        final ObservableList<MockCallLogResponse> logsData = FXCollections.observableArrayList();

        // Header section
        Label iconLabel = new Label("🖧");
        iconLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: -accent;");

        Label nameLabel = new Label(server.getName());
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -text-primary;");

        Label descLabel = new Label(server.getDescription() != null && !server.getDescription().isBlank()
                ? server.getDescription() : "Simulates endpoints and canned responses for frontend, tests, and API workflows.");
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-muted;");
        descLabel.setWrapText(true);

        Label statusBadge = new Label();
        statusBadge.getStyleClass().add("mock-status-pill");
        updateStatusBadge(statusBadge, isEnabled[0]);

        CheckBox activeSwitch = new CheckBox("Server Active");
        activeSwitch.setSelected(isEnabled[0]);
        activeSwitch.selectedProperty().addListener((obs, was, isNow) -> {
            isEnabled[0] = isNow;
            updateStatusBadge(statusBadge, isNow);
            new Thread(() -> {
                boolean ok = MockServerService.setEnabled(server.getId(), isNow);
                if (ok && onUpdated != null) {
                    onUpdated.accept(server);
                }
            }).start();
        });

        Button addRouteTopBtn = new Button("+ Add Route");
        addRouteTopBtn.getStyleClass().add("primary-btn");
        addRouteTopBtn.setMinWidth(Region.USE_PREF_SIZE);

        Button importFromCollectionBtn = new Button("⚡ Import from Collection");
        importFromCollectionBtn.setMinWidth(Region.USE_PREF_SIZE);

        Button deleteServerBtn = new Button("🗑 Delete Server");
        deleteServerBtn.getStyleClass().add("danger-menu-item");
        deleteServerBtn.setMinWidth(Region.USE_PREF_SIZE);

        statusBadge.setMinWidth(Region.USE_PREF_SIZE);
        activeSwitch.setMinWidth(Region.USE_PREF_SIZE);

        HBox titleRow = new HBox(10, iconLabel, nameLabel, statusBadge, activeSwitch);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        HBox topButtons = new HBox(8, addRouteTopBtn, importFromCollectionBtn, deleteServerBtn);
        topButtons.setAlignment(Pos.CENTER_RIGHT);

        HBox topRow = new HBox(12, titleRow, headerSpacer, topButtons);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox topBar = new VBox(6, topRow, descLabel);
        topBar.setPadding(new Insets(10, 16, 10, 16));
        topBar.setStyle("-fx-background-color: -bg-panel; -fx-border-color: -border-color; -fx-border-width: 0 0 1 0;");

        // Postman-style Metadata Bar (as shown in Postman media_1792722410569_4d28fa43.png)
        Label typeLabel = new Label("Mock server type");
        typeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted; -fx-font-weight: bold;");
        Label typeVal = new Label("Public");
        typeVal.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: -text-primary;");
        VBox typeBox = new VBox(2, typeLabel, typeVal);

        Label collLabel = new Label("Collection");
        collLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted; -fx-font-weight: bold;");
        Hyperlink collLink = new Hyperlink("Associate / Import Collection");
        collLink.setStyle("-fx-font-size: 12px; -fx-text-fill: -accent; -fx-padding: 0;");
        collLink.setOnAction(e -> openCollectionImporter(server.getId(), ownerWindow, () -> reloadRoutes(server.getId(), routesData)));
        VBox collBox = new VBox(2, collLabel, collLink);

        Label envLabel = new Label("Environment");
        envLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted; -fx-font-weight: bold;");
        Label envVal = new Label("No environment");
        envVal.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-secondary;");
        VBox envBox = new VBox(2, envLabel, envVal);

        HBox metaBar = new HBox(28, typeBox, collBox, envBox);
        metaBar.setAlignment(Pos.CENTER_LEFT);
        metaBar.setPadding(new Insets(8, 16, 8, 16));
        metaBar.setStyle("-fx-background-color: -bg-secondary; -fx-border-color: -border-color; -fx-border-width: 0 0 1 0;");

        // Base URL Banner
        Label urlTag = new Label("BASE URL");
        urlTag.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -text-muted;");

        TextField baseUrlField = new TextField(server.getBaseUrl());
        baseUrlField.setEditable(false);
        baseUrlField.setStyle("-fx-font-family: Consolas, Monaco, monospace; -fx-font-size: 12px; -fx-background-color: -bg-panel; -fx-text-fill: -text-primary;");
        HBox.setHgrow(baseUrlField, Priority.ALWAYS);

        Button copyUrlBtn = new Button("📋 Copy URL");
        copyUrlBtn.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(server.getBaseUrl());
            Clipboard.getSystemClipboard().setContent(content);
            copyUrlBtn.setText("✓ Copied!");
            copyUrlBtn.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
            Timeline resetTimeline = new Timeline(new KeyFrame(Duration.seconds(2), evt -> {
                copyUrlBtn.setText("📋 Copy URL");
                copyUrlBtn.setStyle("");
            }));
            resetTimeline.play();
        });

        Button testInTabBtn = new Button("⚡ Test in Tab");
        testInTabBtn.setTooltip(new Tooltip("Open this mock server in a new request tab"));
        testInTabBtn.setOnAction(e -> {
            if (onTestInTab != null) {
                String firstPath = routesData.isEmpty() ? "" : routesData.get(0).getPath();
                String testUrl = server.getBaseUrl() + (firstPath.startsWith("/") ? firstPath : "/" + firstPath);
                String method = routesData.isEmpty() ? "GET" : routesData.get(0).getMethod();
                onTestInTab.accept(method, testUrl);
            }
        });

        HBox urlCard = new HBox(10, urlTag, baseUrlField, copyUrlBtn, testInTabBtn);
        urlCard.setAlignment(Pos.CENTER_LEFT);
        urlCard.setPadding(new Insets(8, 14, 8, 14));
        urlCard.setStyle("-fx-background-color: -bg-elevated; -fx-border-color: -border-color; -fx-border-radius: 6px; -fx-background-radius: 6px;");

        // Tabs
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // ---------------- TAB 1: ROUTES ----------------
        Tab routesTab = new Tab("🎭 Configured Routes");
        VBox routesContent = new VBox(10);
        routesContent.setPadding(new Insets(12, 16, 12, 16));

        TextField routeSearchField = new TextField();
        routeSearchField.setPromptText("Filter routes by path or method...");
        routeSearchField.setStyle("-fx-background-color: -bg-elevated; -fx-text-fill: -text-primary;");

        TableView<MockRouteResponse> routesTable = new TableView<>();
        routesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(routesTable, Priority.ALWAYS);

        VBox emptyRoutesPlaceholder = new VBox(8);
        emptyRoutesPlaceholder.setAlignment(Pos.CENTER);
        Label routeIcon = new Label("🎭");
        routeIcon.setStyle("-fx-font-size: 32px;");
        Label routePrompt = new Label("No mock routes configured yet");
        routePrompt.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -text-primary;");
        Label routeSub = new Label("Click '+ Add Route' or '⚡ Import from Collection' to define endpoints.");
        routeSub.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-muted;");
        emptyRoutesPlaceholder.getChildren().addAll(routeIcon, routePrompt, routeSub);
        routesTable.setPlaceholder(emptyRoutesPlaceholder);

        TableColumn<MockRouteResponse, String> methodCol = new TableColumn<>("Method");
        methodCol.setPrefWidth(90);
        methodCol.setMinWidth(80);
        methodCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMethod()));
        methodCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String method, boolean empty) {
                super.updateItem(method, empty);
                if (empty || method == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label chip = new Label(method);
                    chip.getStyleClass().addAll("method-chip", "method-chip-" + method.toLowerCase(Locale.ROOT));
                    chip.setMinWidth(48);
                    chip.setAlignment(Pos.CENTER);
                    setGraphic(chip);
                    setText(null);
                }
            }
        });

        TableColumn<MockRouteResponse, String> pathCol = new TableColumn<>("Path Pattern");
        pathCol.setPrefWidth(240);
        pathCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPath()));
        pathCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String path, boolean empty) {
                super.updateItem(path, empty);
                if (empty || path == null) {
                    setText(null);
                } else {
                    setText(path);
                    setStyle("-fx-font-weight: bold; -fx-font-family: Consolas, Monaco, monospace; -fx-text-fill: -text-primary;");
                }
            }
        });

        TableColumn<MockRouteResponse, String> statusCol = new TableColumn<>("Response Status");
        statusCol.setPrefWidth(120);
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(formatStatus(data.getValue().getResponseStatus())));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label chip = new Label(status);
                    chip.getStyleClass().add("history-status-chip");
                    if (status.startsWith("2")) {
                        chip.getStyleClass().add("status-2xx");
                    } else if (status.startsWith("4") || status.startsWith("5")) {
                        chip.getStyleClass().add("status-4xx");
                    }
                    setGraphic(chip);
                    setText(null);
                }
            }
        });

        TableColumn<MockRouteResponse, String> delayCol = new TableColumn<>("Delay");
        delayCol.setPrefWidth(80);
        delayCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDelayMs() > 0 ? data.getValue().getDelayMs() + "ms" : "-"));

        TableColumn<MockRouteResponse, String> bodyPreviewCol = new TableColumn<>("Response Preview");
        bodyPreviewCol.setPrefWidth(220);
        bodyPreviewCol.setCellValueFactory(data -> {
            String b = data.getValue().getResponseBody();
            if (b == null || b.isBlank()) return new SimpleStringProperty("(empty body)");
            String clean = b.replaceAll("\\s+", " ").trim();
            if (clean.length() > 60) clean = clean.substring(0, 57) + "...";
            return new SimpleStringProperty(clean);
        });
        bodyPreviewCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-family: Consolas, Monaco, monospace; -fx-font-size: 11px; -fx-text-fill: -text-secondary;");
                }
            }
        });

        TableColumn<MockRouteResponse, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(150);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button testBtn = new Button("⚡ Test");
            private final Button editBtn = new Button("✏ Edit");
            private final Button delBtn = new Button("🗑");
            private final HBox btnBox = new HBox(4, testBtn, editBtn, delBtn);

            {
                btnBox.setAlignment(Pos.CENTER_LEFT);
                testBtn.getStyleClass().add("tree-inline-btn");
                testBtn.setTooltip(new Tooltip("Test this route in a new request tab"));
                editBtn.getStyleClass().add("tree-inline-btn");
                delBtn.getStyleClass().addAll("tree-inline-btn", "tree-delete-btn");

                testBtn.setOnAction(e -> {
                    MockRouteResponse r = getTableView().getItems().get(getIndex());
                    if (r != null && onTestInTab != null) {
                        String testUrl = server.getBaseUrl() + (r.getPath().startsWith("/") ? r.getPath() : "/" + r.getPath());
                        onTestInTab.accept(r.getMethod(), testUrl);
                    }
                });

                editBtn.setOnAction(e -> {
                    MockRouteResponse r = getTableView().getItems().get(getIndex());
                    if (r != null) {
                        openRouteEditor(server, r, ownerWindow, () -> reloadRoutes(server.getId(), routesData));
                    }
                });

                delBtn.setOnAction(e -> {
                    MockRouteResponse r = getTableView().getItems().get(getIndex());
                    if (r != null && AlertUtils.showConfirmation("Delete Mock Route",
                            "Delete route " + r.getMethod() + " " + r.getPath() + "?")) {
                        new Thread(() -> {
                            boolean ok = MockServerService.deleteRoute(server.getId(), r.getId());
                            if (ok) {
                                reloadRoutes(server.getId(), routesData);
                            }
                        }).start();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnBox);
            }
        });

        routesTable.getColumns().addAll(methodCol, pathCol, statusCol, delayCol, bodyPreviewCol, actionCol);

        FilteredList<MockRouteResponse> filteredRoutes = new FilteredList<>(routesData, p -> true);
        routeSearchField.textProperty().addListener((obs, oldV, newV) -> {
            filteredRoutes.setPredicate(route -> {
                if (newV == null || newV.isBlank()) return true;
                String lower = newV.trim().toLowerCase(Locale.ROOT);
                return (route.getPath() != null && route.getPath().toLowerCase(Locale.ROOT).contains(lower))
                        || (route.getMethod() != null && route.getMethod().toLowerCase(Locale.ROOT).contains(lower))
                        || String.valueOf(route.getResponseStatus()).contains(lower);
            });
        });
        routesTable.setItems(filteredRoutes);

        HBox routesStatsRow = new HBox(12);
        Label routesCountLabel = new Label();
        routesCountLabel.setStyle("-fx-text-fill: -text-muted; -fx-font-size: 11px;");
        routesData.addListener((javafx.beans.Observable o) ->
                routesCountLabel.setText(routesData.size() + " route" + (routesData.size() == 1 ? "" : "s") + " configured"));
        routesStatsRow.getChildren().add(routesCountLabel);

        routesContent.getChildren().addAll(routeSearchField, routesTable, routesStatsRow);
        routesTab.setContent(routesContent);

        // ---------------- TAB 2: CALL LOG (LIVE INSPECTOR) ----------------
        Tab logsTab = new Tab("📡 Call Log (Live Inspector)");
        VBox logsContent = new VBox(10);
        logsContent.setPadding(new Insets(12, 16, 12, 16));

        Label logsCountLabel = new Label("0 requests logged");
        logsCountLabel.setStyle("-fx-text-fill: -text-muted; -fx-font-size: 12px;");

        Button refreshLogsBtn = new Button("⟳ Refresh");
        CheckBox autoRefreshCheck = new CheckBox("Auto-refresh (3s)");
        autoRefreshCheck.setSelected(true);

        Button clearLogsBtn = new Button("🗑 Clear Logs");
        clearLogsBtn.getStyleClass().add("tree-delete-btn");

        Region logSpacer = new Region();
        HBox.setHgrow(logSpacer, Priority.ALWAYS);

        HBox logsToolbar = new HBox(10, logsCountLabel, logSpacer, autoRefreshCheck, refreshLogsBtn, clearLogsBtn);
        logsToolbar.setAlignment(Pos.CENTER_LEFT);

        SplitPane logsSplit = new SplitPane();
        logsSplit.setOrientation(Orientation.HORIZONTAL);
        logsSplit.setDividerPositions(0.45);
        VBox.setVgrow(logsSplit, Priority.ALWAYS);

        TableView<MockCallLogResponse> logsTable = new TableView<>();
        logsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<MockCallLogResponse, String> logMethodCol = new TableColumn<>("Method");
        logMethodCol.setPrefWidth(75);
        logMethodCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMethod()));
        logMethodCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String method, boolean empty) {
                super.updateItem(method, empty);
                if (empty || method == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label chip = new Label(method);
                    chip.getStyleClass().addAll("method-chip", "method-chip-" + method.toLowerCase(Locale.ROOT));
                    chip.setMinWidth(48);
                    chip.setAlignment(Pos.CENTER);
                    setGraphic(chip);
                    setText(null);
                }
            }
        });

        TableColumn<MockCallLogResponse, String> logPathCol = new TableColumn<>("Path");
        logPathCol.setPrefWidth(140);
        logPathCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPath()));
        logPathCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String path, boolean empty) {
                super.updateItem(path, empty);
                if (empty || path == null) {
                    setText(null);
                } else {
                    setText(path);
                    setStyle("-fx-font-weight: bold; -fx-font-family: Consolas, Monaco, monospace; -fx-text-fill: -text-primary;");
                }
            }
        });

        TableColumn<MockCallLogResponse, String> logStatusCol = new TableColumn<>("Status");
        logStatusCol.setPrefWidth(80);
        logStatusCol.setCellValueFactory(d -> new SimpleStringProperty(formatStatus(d.getValue().getResponseStatus())));
        logStatusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label chip = new Label(status);
                    chip.getStyleClass().add("history-status-chip");
                    if (status.startsWith("2")) {
                        chip.getStyleClass().add("status-2xx");
                    } else if (status.startsWith("4") || status.startsWith("5")) {
                        chip.getStyleClass().add("status-4xx");
                    }
                    setGraphic(chip);
                    setText(null);
                }
            }
        });

        TableColumn<MockCallLogResponse, String> logDurationCol = new TableColumn<>("Time");
        logDurationCol.setPrefWidth(70);
        logDurationCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDurationMs() + "ms"));

        logsTable.getColumns().addAll(logMethodCol, logPathCol, logStatusCol, logDurationCol);
        logsTable.setItems(logsData);

        // Inspector side
        VBox inspectorBox = new VBox(8);
        inspectorBox.setPadding(new Insets(10));
        inspectorBox.setStyle("-fx-background-color: -bg-panel; -fx-border-color: -border-color; -fx-border-radius: 4px;");

        Label inspectorTitle = new Label("Select a call from the list to inspect details");
        inspectorTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: -text-primary; -fx-font-size: 13px;");

        TextArea reqBodyArea = new TextArea();
        reqBodyArea.setEditable(false);
        reqBodyArea.setPromptText("(No request body)");
        reqBodyArea.setPrefRowCount(5);
        reqBodyArea.setStyle("-fx-font-family: Consolas, Monaco, monospace; -fx-font-size: 11px;");

        TextArea reqHeadersArea = new TextArea();
        reqHeadersArea.setEditable(false);
        reqHeadersArea.setPromptText("(No request headers)");
        reqHeadersArea.setPrefRowCount(3);
        reqHeadersArea.setStyle("-fx-font-family: Consolas, Monaco, monospace; -fx-font-size: 11px;");

        TextArea resBodyArea = new TextArea();
        resBodyArea.setEditable(false);
        resBodyArea.setPromptText("(No response body)");
        resBodyArea.setPrefRowCount(8);
        resBodyArea.setStyle("-fx-font-family: Consolas, Monaco, monospace; -fx-font-size: 11px;");
        VBox.setVgrow(resBodyArea, Priority.ALWAYS);

        inspectorBox.getChildren().addAll(
                inspectorTitle,
                new Label("Request Headers:"), reqHeadersArea,
                new Label("Request Body:"), reqBodyArea,
                new Label("Response Served:"), resBodyArea
        );

        logsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            if (selected != null) {
                inspectorTitle.setText(selected.getMethod() + " " + selected.getPath() +
                        "  →  Status " + selected.getResponseStatus() + " (" + selected.getDurationMs() + "ms)");
                reqHeadersArea.setText(formatJsonSafe(selected.getRequestHeaders()));
                reqBodyArea.setText(selected.getRequestBody() != null ? selected.getRequestBody() : "");
                resBodyArea.setText(selected.getResponseBody() != null ? selected.getResponseBody() : "");
            } else {
                inspectorTitle.setText("Select a call from the list to inspect details");
                reqHeadersArea.clear();
                reqBodyArea.clear();
                resBodyArea.clear();
            }
        });

        logsSplit.getItems().addAll(logsTable, inspectorBox);

        // Astronaut Empty State (as in Postman media_1792722410569_4d28fa43.png)
        VBox emptyLogsBox = new VBox(14);
        emptyLogsBox.setAlignment(Pos.CENTER);
        emptyLogsBox.setPadding(new Insets(30, 20, 30, 20));

        Label astronautIcon = new Label("👨‍🚀");
        astronautIcon.setStyle("-fx-font-size: 48px; -fx-padding: 12px; -fx-background-color: rgba(99, 102, 241, 0.12); -fx-background-radius: 50%;");

        Label emptyTitle = new Label("No mock server calls yet");
        emptyTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -text-primary;");

        Label emptySubtitle = new Label("To make your first call to this mock server:");
        emptySubtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-secondary;");

        VBox stepsBox = new VBox(10);
        stepsBox.setMaxWidth(480);
        stepsBox.setPadding(new Insets(14));
        stepsBox.setStyle("-fx-background-color: -bg-elevated; -fx-border-color: -border-color; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        Label step1 = new Label("1. Copy the mock URL:");
        step1.setStyle("-fx-font-weight: bold; -fx-text-fill: -text-primary; -fx-font-size: 12px;");

        TextField emptyUrlField = new TextField(server.getBaseUrl());
        emptyUrlField.setEditable(false);
        emptyUrlField.setStyle("-fx-font-family: Consolas, Monaco, monospace; -fx-font-size: 11px;");
        Button emptyCopyBtn = new Button("📋 Copy URL");
        emptyCopyBtn.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(server.getBaseUrl());
            Clipboard.getSystemClipboard().setContent(content);
            emptyCopyBtn.setText("✓ Copied!");
            Timeline t = new Timeline(new KeyFrame(Duration.seconds(2), evt -> emptyCopyBtn.setText("📋 Copy URL")));
            t.play();
        });
        HBox step1Box = new HBox(8, emptyUrlField, emptyCopyBtn);
        HBox.setHgrow(emptyUrlField, Priority.ALWAYS);
        step1Box.setAlignment(Pos.CENTER_LEFT);

        Label step2 = new Label("2. Send a request to any path configured in your mock server");
        step2.setStyle("-fx-font-weight: bold; -fx-text-fill: -text-primary; -fx-font-size: 12px;");

        Label step2Hint = new Label("Use Thundercall's request editor, cURL, or your application code.");
        step2Hint.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");

        stepsBox.getChildren().addAll(step1, step1Box, step2, step2Hint);

        Button emptyTestBtn = new Button("⚡ Test First Route in Tab");
        emptyTestBtn.getStyleClass().add("primary-btn");
        emptyTestBtn.setOnAction(e -> {
            if (onTestInTab != null) {
                String firstPath = routesData.isEmpty() ? "/example" : routesData.get(0).getPath();
                String testUrl = server.getBaseUrl() + (firstPath.startsWith("/") ? firstPath : "/" + firstPath);
                String method = routesData.isEmpty() ? "GET" : routesData.get(0).getMethod();
                onTestInTab.accept(method, testUrl);
            }
        });

        Button emptyAddRouteBtn = new Button("+ Add Mock Route");
        emptyAddRouteBtn.setOnAction(e -> openRouteEditor(server, null, ownerWindow, () -> reloadRoutes(server.getId(), routesData)));

        HBox emptyActionsRow = new HBox(10, emptyTestBtn, emptyAddRouteBtn);
        emptyActionsRow.setAlignment(Pos.CENTER);

        emptyLogsBox.getChildren().addAll(astronautIcon, emptyTitle, emptySubtitle, stepsBox, emptyActionsRow);

        StackPane logsStack = new StackPane(emptyLogsBox, logsSplit);
        VBox.setVgrow(logsStack, Priority.ALWAYS);

        Runnable updateLogsVisibility = () -> {
            boolean hasLogs = !logsData.isEmpty();
            emptyLogsBox.setVisible(!hasLogs);
            emptyLogsBox.setManaged(!hasLogs);
            logsSplit.setVisible(hasLogs);
            logsSplit.setManaged(hasLogs);
        };
        updateLogsVisibility.run();
        logsData.addListener((javafx.beans.Observable o) -> updateLogsVisibility.run());

        logsContent.getChildren().addAll(logsToolbar, logsStack);
        logsTab.setContent(logsContent);

        // ---------------- TAB 3: CODE SNIPPETS ----------------
        Tab snippetsTab = new Tab("💻 Code Snippets");
        VBox snippetsContent = new VBox(12);
        snippetsContent.setPadding(new Insets(16));

        Label snippetsHint = new Label("Ready-to-use snippets to call this mock server from your frontend, backend, or terminal:");
        snippetsHint.setStyle("-fx-text-fill: -text-secondary; -fx-font-size: 13px;");

        ComboBox<String> snippetTypeCombo = new ComboBox<>();
        snippetTypeCombo.getItems().addAll("cURL", "JavaScript (Fetch)", "JavaScript (Axios)", "Python (Requests)");
        snippetTypeCombo.setValue("cURL");

        TextArea snippetCodeArea = new TextArea();
        snippetCodeArea.setEditable(false);
        snippetCodeArea.setStyle("-fx-font-family: Consolas, Monaco, monospace; -fx-font-size: 12px;");
        snippetCodeArea.setPrefRowCount(14);
        VBox.setVgrow(snippetCodeArea, Priority.ALWAYS);

        Button copySnippetBtn = new Button("📋 Copy Snippet");
        copySnippetBtn.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(snippetCodeArea.getText());
            Clipboard.getSystemClipboard().setContent(content);
            copySnippetBtn.setText("✓ Copied!");
            Timeline t = new Timeline(new KeyFrame(Duration.seconds(2), evt -> copySnippetBtn.setText("📋 Copy Snippet")));
            t.play();
        });

        Runnable updateSnippets = () -> {
            String path = routesData.isEmpty() ? "/example" : routesData.get(0).getPath();
            String method = routesData.isEmpty() ? "GET" : routesData.get(0).getMethod();
            String fullUrl = server.getBaseUrl() + (path.startsWith("/") ? path : "/" + path);
            String selected = snippetTypeCombo.getValue();
            if ("cURL".equals(selected)) {
                snippetCodeArea.setText("curl -X " + method + " \"" + fullUrl + "\" \\\n  -H \"Content-Type: application/json\"");
            } else if ("JavaScript (Fetch)".equals(selected)) {
                snippetCodeArea.setText("fetch(\"" + fullUrl + "\", {\n"
                        + "  method: \"" + method + "\",\n"
                        + "  headers: {\n    \"Content-Type\": \"application/json\"\n  }\n"
                        + "})\n"
                        + ".then(response => response.json())\n"
                        + ".then(data => console.log(data))\n"
                        + ".catch(error => console.error(\"Error:\", error));");
            } else if ("JavaScript (Axios)".equals(selected)) {
                snippetCodeArea.setText("import axios from 'axios';\n\n"
                        + "axios." + method.toLowerCase(Locale.ROOT) + "(\"" + fullUrl + "\")\n"
                        + "  .then(response => {\n    console.log(response.data);\n  })\n"
                        + "  .catch(error => {\n    console.error(error);\n  });");
            } else if ("Python (Requests)".equals(selected)) {
                snippetCodeArea.setText("import requests\n\n"
                        + "response = requests." + method.toLowerCase(Locale.ROOT) + "(\"" + fullUrl + "\")\n"
                        + "print(response.status_code)\n"
                        + "print(response.json())");
            }
        };

        snippetTypeCombo.valueProperty().addListener((obs, oldV, newV) -> updateSnippets.run());
        updateSnippets.run();

        HBox snippetTopRow = new HBox(10, new Label("Format:"), snippetTypeCombo, copySnippetBtn);
        snippetTopRow.setAlignment(Pos.CENTER_LEFT);

        snippetsContent.getChildren().addAll(snippetsHint, snippetTopRow, snippetCodeArea);
        snippetsTab.setContent(snippetsContent);

        tabPane.getTabs().addAll(routesTab, logsTab, snippetsTab);

        // Actions wiring
        Runnable refreshRoutesAction = () -> reloadRoutes(server.getId(), routesData);
        Runnable refreshLogsAction = () -> reloadLogs(server.getId(), logsData, logsCountLabel);

        refreshLogsBtn.setOnAction(e -> refreshLogsAction.run());
        clearLogsBtn.setOnAction(e -> {
            if (AlertUtils.showConfirmation("Clear Logs", "Clear all recorded calls for this mock server?")) {
                new Thread(() -> {
                    boolean ok = MockServerService.clearCallLogs(server.getId());
                    if (ok) {
                        Platform.runLater(refreshLogsAction);
                    }
                }).start();
            }
        });

        addRouteTopBtn.setOnAction(e -> openRouteEditor(server, null, ownerWindow, refreshRoutesAction));

        importFromCollectionBtn.setOnAction(e -> openCollectionImporter(server.getId(), ownerWindow, refreshRoutesAction));

        deleteServerBtn.setOnAction(e -> {
            if (AlertUtils.showConfirmation("Delete Mock Server",
                    "Delete \"" + server.getName() + "\" and all its routes? This cannot be undone.")) {
                new Thread(() -> {
                    boolean ok = MockServerService.deleteMockServer(server.getId());
                    Platform.runLater(() -> {
                        if (ok) {
                            dialog.close();
                            if (onUpdated != null) onUpdated.accept(null);
                        }
                    });
                }).start();
            }
        });

        // Auto-refresh timeline for live logs
        Timeline autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(3), evt -> {
            if (autoRefreshCheck.isSelected() && tabPane.getSelectionModel().getSelectedItem() == logsTab) {
                refreshLogsAction.run();
            }
        }));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();

        dialog.setOnHidden(e -> autoRefreshTimeline.stop());

        // Main Layout Container
        VBox root = new VBox(10, topBar, metaBar, new VBox(8, urlCard, tabPane));
        root.setPadding(new Insets(10));
        root.setPrefSize(960, 680);
        pane.setContent(root);

        // Initial data loads
        refreshRoutesAction.run();
        refreshLogsAction.run();

        dialog.showAndWait();
    }

    private static void updateStatusBadge(Label badge, boolean enabled) {
        badge.setText(enabled ? "● Active" : "○ Inactive");
        badge.setStyle(enabled
                ? "-fx-background-color: rgba(16, 185, 129, 0.16); -fx-text-fill: #10b981; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 12px; -fx-border-color: rgba(16, 185, 129, 0.4); -fx-border-radius: 12px;"
                : "-fx-background-color: rgba(148, 163, 184, 0.16); -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 12px; -fx-border-color: rgba(148, 163, 184, 0.4); -fx-border-radius: 12px;");
    }

    private static void reloadRoutes(Long mockServerId, ObservableList<MockRouteResponse> list) {
        new Thread(() -> {
            Optional<List<MockRouteResponse>> routes = MockServerService.getRoutes(mockServerId);
            Platform.runLater(() -> routes.ifPresent(list::setAll));
        }).start();
    }

    private static void reloadLogs(Long mockServerId, ObservableList<MockCallLogResponse> list, Label countLabel) {
        new Thread(() -> {
            Optional<List<MockCallLogResponse>> logs = MockServerService.getCallLogs(mockServerId, 100);
            Platform.runLater(() -> {
                logs.ifPresent(items -> {
                    list.setAll(items);
                    if (countLabel != null) {
                        countLabel.setText(items.size() + " request" + (items.size() == 1 ? "" : "s") + " logged");
                    }
                });
            });
        }).start();
    }

    private static void openRouteEditor(MockServerResponse server, MockRouteResponse existing, Window owner, Runnable onSaved) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Mock Route" : "Edit Mock Route — " + existing.getMethod() + " " + existing.getPath());
        if (owner != null) dialog.initOwner(owner);
        dialog.setResizable(true);

        ButtonType saveBtnType = new ButtonType(existing == null ? "Add Route" : "Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, saveBtnType);
        ThemeManager.styleDialog(dialog.getDialogPane());

        dialog.setWidth(880);
        dialog.setHeight(680);
        dialog.getDialogPane().setPrefSize(880, 680);
        dialog.getDialogPane().setMinWidth(780);
        dialog.getDialogPane().setMinHeight(560);

        dialog.setOnShowing(e -> {
            Window win = dialog.getDialogPane().getScene() != null ? dialog.getDialogPane().getScene().getWindow() : null;
            if (win instanceof javafx.stage.Stage stage) {
                stage.setWidth(880);
                stage.setHeight(680);
                stage.setMinWidth(780);
                stage.setMinHeight(560);
                stage.centerOnScreen();
            }
        });

        Node saveBtnNode = dialog.getDialogPane().lookupButton(saveBtnType);
        if (saveBtnNode != null) {
            saveBtnNode.getStyleClass().add("primary-btn");
        }

        // 1. Header Banner
        Label titleIcon = new Label(existing == null ? "✨" : "✏");
        titleIcon.setStyle("-fx-font-size: 20px;");
        Label titleText = new Label(existing == null ? "Add Mock Route" : "Edit Mock Route");
        titleText.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -text-primary;");
        Label descText = new Label("Define HTTP method, endpoint pattern, response status code, simulated latency, and canned payload.");
        descText.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");

        VBox headerBox = new VBox(2, new HBox(8, titleIcon, titleText), descText);
        headerBox.setPadding(new Insets(4, 0, 8, 0));

        // Form controls declared early for presets wiring
        ComboBox<String> methodCombo = new ComboBox<>();
        methodCombo.getItems().addAll("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS");
        methodCombo.setValue(existing != null ? existing.getMethod() : "GET");
        methodCombo.setMinWidth(110);

        TextField pathField = new TextField(existing != null ? existing.getPath() : "/users");
        pathField.setPromptText("/users or /users/:id or /users/*");
        pathField.setStyle("-fx-font-family: Consolas, Monaco, monospace; -fx-font-size: 13px; -fx-font-weight: bold;");
        HBox.setHgrow(pathField, Priority.ALWAYS);

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(
                "200 OK", "201 Created", "204 No Content",
                "400 Bad Request", "401 Unauthorized", "403 Forbidden",
                "404 Not Found", "500 Internal Server Error");
        statusCombo.setEditable(true);
        statusCombo.setValue(existing != null ? formatStatus(existing.getResponseStatus()) : "200 OK");
        statusCombo.setPrefWidth(160);

        TextField delayField = new TextField(existing != null ? String.valueOf(existing.getDelayMs()) : "0");
        delayField.setPrefWidth(65);
        delayField.setStyle("-fx-font-family: Consolas, Monaco, monospace; -fx-alignment: CENTER-RIGHT;");

        TextArea bodyArea = new TextArea(existing != null ? existing.getResponseBody() : "{\n  \"message\": \"success\",\n  \"data\": {\n    \"id\": \"{{$randomUUID}}\",\n    \"name\": \"{{$randomFullName}}\",\n    \"email\": \"{{$randomEmail}}\"\n  }\n}");
        bodyArea.setStyle("-fx-font-family: Consolas, Monaco, monospace; -fx-font-size: 12px;");
        VBox.setVgrow(bodyArea, Priority.ALWAYS);

        TextArea headersArea = new TextArea(existing != null ? existing.getResponseHeaders() : "{\n  \"Content-Type\": \"application/json\"\n}");
        headersArea.setStyle("-fx-font-family: Consolas, Monaco, monospace; -fx-font-size: 11px;");
        headersArea.setPrefRowCount(6);
        VBox.setVgrow(headersArea, Priority.ALWAYS);

        // 2. Interactive Presets Toolbar
        Label presetsLabel = new Label("⚡ Quick Presets:");
        presetsLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -text-muted;");

        Button pSingle = new Button("📄 Single Item (GET)");
        Button pList = new Button("📋 List Array (GET)");
        Button pCreate = new Button("➕ Create (POST)");
        Button pNotFound = new Button("⚠️ 404 Error");
        Button pServerError = new Button("⚡ 500 Error");

        for (Button b : Arrays.asList(pSingle, pList, pCreate, pNotFound, pServerError)) {
            b.setStyle("-fx-font-size: 11px; -fx-padding: 3 8 3 8; -fx-background-radius: 12px; -fx-cursor: hand;");
        }

        pSingle.setOnAction(e -> {
            methodCombo.setValue("GET");
            pathField.setText("/users/:id");
            statusCombo.setValue("200 OK");
            bodyArea.setText("{\n  \"id\": \"{{id}}\",\n  \"name\": \"{{$randomFullName}}\",\n  \"email\": \"{{$randomEmail}}\",\n  \"phone\": \"{{$randomPhone}}\",\n  \"role\": \"Software Engineer\",\n  \"createdAt\": \"{{$isoTimestamp}}\"\n}");
            delayField.setText("0");
        });

        pList.setOnAction(e -> {
            methodCombo.setValue("GET");
            pathField.setText("/users");
            statusCombo.setValue("200 OK");
            bodyArea.setText("{\n  \"total\": 3,\n  \"page\": 1,\n  \"data\": [\n    {\n      \"id\": \"{{$randomUUID}}\",\n      \"name\": \"{{$randomFullName}}\",\n      \"email\": \"{{$randomEmail}}\",\n      \"price\": {{$randomPrice}}\n    },\n    {\n      \"id\": \"{{$randomUUID}}\",\n      \"name\": \"{{$randomFullName}}\",\n      \"email\": \"{{$randomEmail}}\",\n      \"price\": {{$randomPrice}}\n    },\n    {\n      \"id\": \"{{$randomUUID}}\",\n      \"name\": \"{{$randomFullName}}\",\n      \"email\": \"{{$randomEmail}}\",\n      \"price\": {{$randomPrice}}\n    }\n  ]\n}");
            delayField.setText("0");
        });

        pCreate.setOnAction(e -> {
            methodCombo.setValue("POST");
            pathField.setText("/users");
            statusCombo.setValue("201 Created");
            bodyArea.setText("{\n  \"success\": true,\n  \"message\": \"User created successfully\",\n  \"id\": \"{{$randomUUID}}\",\n  \"createdAt\": \"{{$isoTimestamp}}\"\n}");
            delayField.setText("50");
        });

        pNotFound.setOnAction(e -> {
            methodCombo.setValue("GET");
            pathField.setText("/items/:id");
            statusCombo.setValue("404 Not Found");
            bodyArea.setText("{\n  \"error\": \"Not Found\",\n  \"status\": 404,\n  \"message\": \"Item with ID {{id}} was not found\",\n  \"timestamp\": \"{{$timestamp}}\"\n}");
            delayField.setText("0");
        });

        pServerError.setOnAction(e -> {
            methodCombo.setValue("GET");
            pathField.setText("/service/test");
            statusCombo.setValue("500 Internal Server Error");
            bodyArea.setText("{\n  \"error\": \"Internal Server Error\",\n  \"status\": 500,\n  \"message\": \"Simulated database connection timeout\",\n  \"timestamp\": \"{{$timestamp}}\"\n}");
            delayField.setText("100");
        });

        HBox presetsRow = new HBox(6, presetsLabel, pSingle, pList, pCreate, pNotFound, pServerError);
        presetsRow.setAlignment(Pos.CENTER_LEFT);
        presetsRow.setPadding(new Insets(6, 10, 6, 10));
        presetsRow.setStyle("-fx-background-color: -bg-secondary; -fx-border-color: -border-color; -fx-border-radius: 6px; -fx-background-radius: 6px;");
        presetsRow.setMinWidth(Region.USE_PREF_SIZE);

        // 3. Endpoint Row
        Runnable updateMethodColor = () -> {
            String m = methodCombo.getValue() != null ? methodCombo.getValue().toUpperCase(Locale.ROOT) : "GET";
            String color = switch (m) {
                case "GET" -> "#10b981";
                case "POST" -> "#3b82f6";
                case "PUT" -> "#f59e0b";
                case "DELETE" -> "#ef4444";
                case "PATCH" -> "#8b5cf6";
                default -> "#6b7280";
            };
            methodCombo.setStyle("-fx-font-weight: bold; -fx-text-fill: " + color + "; -fx-border-color: " + color + "; -fx-border-radius: 4px;");
        };
        methodCombo.valueProperty().addListener((obs, oldV, newV) -> updateMethodColor.run());
        updateMethodColor.run();

        Label baseUrlPrefix = new Label(server.getBaseUrl());
        baseUrlPrefix.setStyle("-fx-font-family: Consolas, Monaco, monospace; -fx-font-size: 11px; -fx-text-fill: -text-muted; -fx-padding: 0 4 0 4;");

        HBox endpointRow = new HBox(8, methodCombo, baseUrlPrefix, pathField);
        endpointRow.setAlignment(Pos.CENTER_LEFT);
        endpointRow.setPadding(new Insets(8, 12, 8, 12));
        endpointRow.setStyle("-fx-background-color: -bg-elevated; -fx-border-color: -border-color; -fx-border-radius: 6px; -fx-background-radius: 6px;");
        endpointRow.setMinWidth(Region.USE_PREF_SIZE);

        // Live URL variable detection
        HBox detectedVariablesBox = new HBox(6);
        detectedVariablesBox.setAlignment(Pos.CENTER_LEFT);
        Label detectedHint = new Label("Supports path variables (:id, {id}) and wildcards (*) — e.g. /users/:id");
        detectedHint.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");
        detectedVariablesBox.getChildren().add(detectedHint);

        Runnable updatePathVars = () -> {
            String p = pathField.getText();
            List<String> vars = new ArrayList<>();
            if (p != null) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("[:{]([a-zA-Z0-9_]+)}?").matcher(p);
                while (m.find()) {
                    String v = m.group(1);
                    if (!vars.contains(v)) vars.add(v);
                }
            }
            detectedVariablesBox.getChildren().clear();
            if (vars.isEmpty()) {
                detectedVariablesBox.getChildren().add(detectedHint);
            } else {
                Label tagLabel = new Label("Detected URL Variables:");
                tagLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -accent;");
                detectedVariablesBox.getChildren().add(tagLabel);
                for (String v : vars) {
                    Button varChip = new Button("+ {{" + v + "}}");
                    varChip.setTooltip(new Tooltip("Click to insert {{" + v + "}} into response body"));
                    varChip.setStyle("-fx-font-size: 11px; -fx-padding: 2 6 2 6; -fx-background-color: rgba(99, 102, 241, 0.15); -fx-text-fill: -accent; -fx-background-radius: 4px; -fx-cursor: hand;");
                    varChip.setOnAction(e -> {
                        int pos = bodyArea.getCaretPosition();
                        bodyArea.insertText(pos, "{{" + v + "}}");
                        bodyArea.requestFocus();
                    });
                    detectedVariablesBox.getChildren().add(varChip);
                }
            }
        };
        pathField.textProperty().addListener((obs, oldV, newV) -> updatePathVars.run());
        updatePathVars.run();

        // 4. Status Code & Latency Bar
        HBox statusPills = new HBox(4);
        statusPills.setAlignment(Pos.CENTER_LEFT);
        String[] commonStatuses = {"200 OK", "201 Created", "204 No Content", "400 Bad Request", "401 Unauthorized", "404 Not Found", "500 Server Error"};
        List<Button> pillButtons = new ArrayList<>();
        for (String cs : commonStatuses) {
            Button pill = new Button(cs.split(" ")[0]);
            pill.setTooltip(new Tooltip(cs));
            pill.setStyle("-fx-font-size: 11px; -fx-padding: 2 6 2 6; -fx-background-radius: 10px; -fx-cursor: hand;");
            pill.setOnAction(e -> statusCombo.setValue(cs.equals("500 Server Error") ? "500 Internal Server Error" : cs));
            pillButtons.add(pill);
            statusPills.getChildren().add(pill);
        }

        Consumer<String> highlightActivePill = val -> {
            for (Button b : pillButtons) {
                boolean active = val != null && val.startsWith(b.getText());
                if (active) {
                    String c = val.startsWith("2") ? "#10b981" : (val.startsWith("4") ? "#f59e0b" : "#ef4444");
                    b.setStyle("-fx-font-size: 11px; -fx-padding: 2 6 2 6; -fx-background-radius: 10px; -fx-background-color: " + c + "; -fx-text-fill: white; -fx-font-weight: bold;");
                } else {
                    b.setStyle("-fx-font-size: 11px; -fx-padding: 2 6 2 6; -fx-background-radius: 10px; -fx-background-color: -bg-secondary; -fx-text-fill: -text-secondary;");
                }
            }
        };
        statusCombo.valueProperty().addListener((obs, oldV, newV) -> highlightActivePill.accept(newV));
        highlightActivePill.accept(statusCombo.getValue());

        HBox delayPresets = new HBox(4,
                createDelayPresetBtn("0ms", "0", delayField),
                createDelayPresetBtn("100ms", "100", delayField),
                createDelayPresetBtn("300ms", "300", delayField),
                createDelayPresetBtn("800ms", "800", delayField),
                createDelayPresetBtn("1.5s", "1500", delayField));
        delayPresets.setAlignment(Pos.CENTER_LEFT);

        Region statusSpacer = new Region();
        HBox.setHgrow(statusSpacer, Priority.ALWAYS);

        HBox statusAndDelayCard = new HBox(16,
                new VBox(4, new Label("Response Status:"), new HBox(6, statusCombo, statusPills)),
                statusSpacer,
                new VBox(4, new Label("Latency / Delay:"), new HBox(6, delayField, new Label("ms"), delayPresets)));
        statusAndDelayCard.setAlignment(Pos.CENTER_LEFT);
        statusAndDelayCard.setPadding(new Insets(8, 12, 8, 12));
        statusAndDelayCard.setStyle("-fx-background-color: -bg-elevated; -fx-border-color: -border-color; -fx-border-radius: 6px; -fx-background-radius: 6px;");
        statusAndDelayCard.setMinWidth(Region.USE_PREF_SIZE);

        // 5. TabPane for Response Body, Headers, Preview
        TabPane payloadTabPane = new TabPane();
        payloadTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        payloadTabPane.setPrefHeight(380);
        payloadTabPane.setMinHeight(280);
        VBox.setVgrow(payloadTabPane, Priority.ALWAYS);

        // TAB 1: Body
        Tab bodyTab = new Tab("📄 Response Body");
        VBox bodyBox = new VBox(8);
        bodyBox.setPadding(new Insets(10));

        Button formatJsonBtn = new Button("{ } Format JSON");
        formatJsonBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 8 3 8;");
        Button clearBodyBtn = new Button("🗑 Clear");
        clearBodyBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 8 3 8;");

        Label jsonValidationLabel = new Label("● Valid JSON");
        jsonValidationLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #10b981;");

        Region bodySpacer = new Region();
        HBox.setHgrow(bodySpacer, Priority.ALWAYS);

        Label contentTypeLabel = new Label("application/json");
        contentTypeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted; -fx-font-family: Consolas, Monaco, monospace;");

        HBox bodyToolbar = new HBox(8, formatJsonBtn, clearBodyBtn, jsonValidationLabel, bodySpacer, contentTypeLabel);
        bodyToolbar.setAlignment(Pos.CENTER_LEFT);

        Runnable checkJsonValid = () -> {
            String t = bodyArea.getText();
            if (t == null || t.isBlank()) {
                jsonValidationLabel.setText("● Empty Body");
                jsonValidationLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");
                return;
            }
            try {
                new JSONObject(t);
                jsonValidationLabel.setText("● Valid JSON Object");
                jsonValidationLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #10b981;");
            } catch (Exception ex1) {
                try {
                    new org.json.JSONArray(t);
                    jsonValidationLabel.setText("● Valid JSON Array");
                    jsonValidationLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #10b981;");
                } catch (Exception ex2) {
                    jsonValidationLabel.setText("● Syntax Warning");
                    jsonValidationLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #f59e0b;");
                }
            }
        };
        bodyArea.textProperty().addListener((obs, oldV, newV) -> checkJsonValid.run());
        checkJsonValid.run();

        formatJsonBtn.setOnAction(e -> {
            String raw = bodyArea.getText();
            if (raw == null || raw.isBlank()) return;
            try {
                if (raw.trim().startsWith("[")) {
                    bodyArea.setText(new org.json.JSONArray(raw).toString(2));
                } else {
                    bodyArea.setText(new JSONObject(raw).toString(2));
                }
            } catch (Exception ignored) {}
        });

        clearBodyBtn.setOnAction(e -> bodyArea.clear());

        VBox fakerShelf = createFakerVariablesShelf(bodyArea);
        bodyBox.getChildren().addAll(bodyToolbar, bodyArea, fakerShelf);
        bodyTab.setContent(bodyBox);

        // TAB 2: Headers
        Tab headersTab = new Tab("📑 Response Headers");
        VBox headersBox = new VBox(8);
        headersBox.setPadding(new Insets(10));

        HBox headerPresets = new HBox(6,
                new Label("Quick Headers:"),
                createHeaderPresetBtn("+ JSON", "Content-Type", "application/json", headersArea),
                createHeaderPresetBtn("+ CORS (Allow All)", "Access-Control-Allow-Origin", "*", headersArea),
                createHeaderPresetBtn("+ No-Cache", "Cache-Control", "no-cache, no-store, must-revalidate", headersArea),
                createHeaderPresetBtn("+ Plain Text", "Content-Type", "text/plain", headersArea)
        );
        headerPresets.setAlignment(Pos.CENTER_LEFT);

        Label headersHint = new Label("Headers must be valid JSON key-value format, e.g. {\"Content-Type\": \"application/json\"}");
        headersHint.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");

        headersBox.getChildren().addAll(headerPresets, headersArea, headersHint);
        headersTab.setContent(headersBox);

        // TAB 3: Preview
        Tab previewTab = new Tab("⚡ Live Preview");
        VBox previewBox = new VBox(12);
        previewBox.setPadding(new Insets(14));

        Label previewHeading = new Label("Route Execution Summary");
        previewHeading.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -text-primary;");

        Label fullUrlPreview = new Label();
        fullUrlPreview.setStyle("-fx-font-family: Consolas, Monaco, monospace; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -accent;");

        Label statusPreview = new Label();
        statusPreview.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-secondary;");

        Label latencyPreview = new Label();
        latencyPreview.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-secondary;");

        Label dynamicTokensPreview = new Label();
        dynamicTokensPreview.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-secondary;");
        dynamicTokensPreview.setWrapText(true);

        Runnable updatePreviewTab = () -> {
            String m = methodCombo.getValue() != null ? methodCombo.getValue() : "GET";
            String p = pathField.getText() != null ? pathField.getText().trim() : "/";
            if (!p.startsWith("/")) p = "/" + p;
            fullUrlPreview.setText(m + " " + server.getBaseUrl() + p);
            statusPreview.setText("Status Code: " + statusCombo.getValue());
            latencyPreview.setText("Simulated Network Latency: " + delayField.getText() + " ms");

            String b = bodyArea.getText();
            List<String> foundTokens = new ArrayList<>();
            if (b != null) {
                java.util.regex.Matcher tm = java.util.regex.Pattern.compile("\\{\\{[^{}]+\\}\\}").matcher(b);
                while (tm.find()) {
                    String tok = tm.group();
                    if (!foundTokens.contains(tok)) foundTokens.add(tok);
                }
            }
            if (foundTokens.isEmpty()) {
                dynamicTokensPreview.setText("Dynamic Tokens: None (Static payload returned)");
            } else {
                dynamicTokensPreview.setText("Dynamic Tokens Detected: " + String.join(", ", foundTokens) +
                        "\n(These tokens will be dynamically evaluated by the mock server engine on each incoming call)");
            }
        };

        payloadTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV == previewTab) updatePreviewTab.run();
        });

        previewBox.getChildren().addAll(
                previewHeading,
                new VBox(4, new Label("Target Mock URL:"), fullUrlPreview),
                statusPreview,
                latencyPreview,
                dynamicTokensPreview
        );
        previewTab.setContent(previewBox);

        payloadTabPane.getTabs().addAll(bodyTab, headersTab, previewTab);

        // Main Dialog Content
        VBox root = new VBox(10,
                headerBox,
                presetsRow,
                endpointRow,
                detectedVariablesBox,
                statusAndDelayCard,
                payloadTabPane
        );
        root.setPadding(new Insets(12));
        root.setPrefSize(880, 680);
        root.setMinWidth(780);
        root.setMinHeight(560);
        dialog.getDialogPane().setContent(root);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == saveBtnType) {
                String path = pathField.getText() == null ? "/" : pathField.getText().trim();
                if (path.isEmpty()) path = "/";
                if (!path.startsWith("/")) path = "/" + path;

                int status = 200;
                String statusStr = statusCombo.getValue();
                if (statusStr != null) {
                    String[] parts = statusStr.trim().split("\\s+");
                    try {
                        status = Integer.parseInt(parts[0]);
                    } catch (Exception ignored) {}
                }

                int delay = 0;
                try {
                    delay = Integer.parseInt(delayField.getText().trim());
                } catch (Exception ignored) {}

                String headersJson = headersArea.getText();
                if (headersJson != null && !headersJson.isBlank()) {
                    try {
                        new JSONObject(headersJson);
                    } catch (Exception ex) {
                        AlertUtils.showError("Headers must be valid JSON, e.g. {\"Content-Type\": \"application/json\"}");
                        return;
                    }
                }

                MockRouteRequest req = new MockRouteRequest(
                        methodCombo.getValue(), path, status, bodyArea.getText(), headersJson, delay);

                new Thread(() -> {
                    try {
                        if (existing == null) {
                            MockServerService.addRoute(server.getId(), req);
                        } else {
                            MockServerService.updateRoute(server.getId(), existing.getId(), req);
                        }
                        Platform.runLater(onSaved);
                    } catch (IOException ex) {
                        Platform.runLater(() -> AlertUtils.showError("Failed to save route: " + MockServerService.friendlyMessage(ex)));
                    }
                }).start();
            }
        });
    }

    private static VBox createFakerVariablesShelf(TextArea bodyArea) {
        VBox shelf = new VBox(6);
        shelf.setPadding(new Insets(8));
        shelf.setStyle("-fx-background-color: -bg-secondary; -fx-border-color: -border-color; -fx-border-radius: 6px; -fx-background-radius: 6px;");

        Label title = new Label("Dynamic Variables (click to insert at cursor):");
        title.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -text-secondary;");

        FlowPane chipsPane = new FlowPane();
        chipsPane.setHgap(6);
        chipsPane.setVgap(6);

        String[][] tokenDefs = {
                {"UUID", "{{$randomUUID}}", "Random UUID v4 string"},
                {"Int", "{{$randomInt}}", "Random integer between 1 and 1000"},
                {"Full Name", "{{$randomFullName}}", "Realistic full name"},
                {"Email", "{{$randomEmail}}", "Realistic email address"},
                {"Phone", "{{$randomPhone}}", "Random phone number"},
                {"Price", "{{$randomPrice}}", "Random price format (e.g. 89.99)"},
                {"Company", "{{$randomCompanyName}}", "Random company name"},
                {"City", "{{$randomCity}}", "Random city name"},
                {"Timestamp", "{{$timestamp}}", "Current Unix epoch timestamp in milliseconds"},
                {"ISO 8601", "{{$isoTimestamp}}", "Current ISO-8601 UTC timestamp string"}
        };

        for (String[] def : tokenDefs) {
            Button chip = new Button("+ " + def[0]);
            chip.setTooltip(new Tooltip(def[1] + " — " + def[2]));
            chip.setStyle("-fx-font-size: 11px; -fx-padding: 3 8 3 8; -fx-background-color: rgba(99, 102, 241, 0.12); -fx-text-fill: -accent; -fx-background-radius: 12px; -fx-cursor: hand;");
            chip.setOnAction(e -> {
                int caret = bodyArea.getCaretPosition();
                bodyArea.insertText(caret, def[1]);
                bodyArea.requestFocus();
            });
            chipsPane.getChildren().add(chip);
        }

        shelf.getChildren().addAll(title, chipsPane);
        return shelf;
    }

    private static Button createHeaderPresetBtn(String label, String key, String val, TextArea area) {
        Button b = new Button(label);
        b.setStyle("-fx-font-size: 11px; -fx-padding: 2 6 2 6; -fx-background-radius: 4px;");
        b.setOnAction(e -> {
            try {
                String cur = area.getText();
                JSONObject obj = (cur != null && !cur.isBlank()) ? new JSONObject(cur) : new JSONObject();
                obj.put(key, val);
                area.setText(obj.toString(2));
            } catch (Exception ex) {
                area.setText("{\n  \"" + key + "\": \"" + val + "\"\n}");
            }
        });
        return b;
    }

    private static Button createDelayPresetBtn(String label, String val, TextField target) {
        Button b = new Button(label);
        b.setStyle("-fx-font-size: 11px; -fx-padding: 2 6 2 6; -fx-background-radius: 4px; -fx-cursor: hand;");
        b.setOnAction(e -> target.setText(val));
        return b;
    }

    private static void openCollectionImporter(Long mockServerId, Window owner, Runnable onImported) {
        Dialog<CollectionResponse> dialog = new Dialog<>();
        dialog.setTitle("Import Routes from Collection");
        if (owner != null) dialog.initOwner(owner);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        ThemeManager.styleDialog(dialog.getDialogPane());

        Label prompt = new Label("Choose a collection to generate mock routes from all its requests:");
        prompt.setStyle("-fx-text-fill: -text-secondary; -fx-font-size: 13px;");

        ListView<CollectionResponse> collectionsList = new ListView<>();
        collectionsList.setPrefHeight(220);
        collectionsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(CollectionResponse c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label name = new Label(c.getName());
                    name.setStyle("-fx-font-weight: bold; -fx-text-fill: -text-primary;");
                    Label meta = new Label(c.getRequestCount() + " requests");
                    meta.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");
                    setGraphic(new VBox(2, name, meta));
                    setText(null);
                }
            }
        });

        new Thread(() -> {
            Optional<List<CollectionResponse>> list = CollectionService.getUserCollections();
            Platform.runLater(() -> list.ifPresent(cols -> {
                collectionsList.getItems().setAll(cols);
                if (!cols.isEmpty()) {
                    collectionsList.getSelectionModel().select(0);
                }
            }));
        }).start();

        VBox content = new VBox(10, prompt, collectionsList);
        content.setPadding(new Insets(12));
        content.setPrefWidth(380);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> btn == ButtonType.OK ? collectionsList.getSelectionModel().getSelectedItem() : null);

        dialog.showAndWait().ifPresent(selected -> {
            if (selected != null) {
                new Thread(() -> {
                    Optional<List<MockRouteResponse>> generated = MockServerService.generateRoutesFromCollection(mockServerId, selected.getId());
                    Platform.runLater(() -> {
                        if (generated.isPresent()) {
                            AlertUtils.showSuccess("Generated " + generated.get().size() + " mock routes from \"" + selected.getName() + "\"!");
                            onImported.run();
                        }
                    });
                }).start();
            }
        });
    }

    private static String formatStatus(int code) {
        return switch (code) {
            case 200 -> "200 OK";
            case 201 -> "201 Created";
            case 204 -> "204 No Content";
            case 400 -> "400 Bad Request";
            case 401 -> "401 Unauthorized";
            case 403 -> "403 Forbidden";
            case 404 -> "404 Not Found";
            case 500 -> "500 Internal Server Error";
            default -> String.valueOf(code);
        };
    }

    private static String formatJsonSafe(String raw) {
        if (raw == null || raw.isBlank()) return "";
        try {
            return new JSONObject(raw).toString(2);
        } catch (Exception e) {
            return raw;
        }
    }
}
