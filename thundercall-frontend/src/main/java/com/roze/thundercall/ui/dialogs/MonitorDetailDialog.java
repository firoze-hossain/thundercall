package com.roze.thundercall.ui.dialogs;

import com.roze.thundercall.ui.models.MonitorResponse;
import com.roze.thundercall.ui.models.MonitorRunResponse;
import com.roze.thundercall.ui.services.MonitorService;
import com.roze.thundercall.ui.utils.AlertUtils;
import com.roze.thundercall.ui.utils.ThemeManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.Window;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

/**
 * Modern, interactive Postman-grade Monitor Dashboard dialog.
 * Features:
 * - Real-time Status Badge & Pause/Resume toggle
 * - Top KPI performance cards (Success Rate, Avg Latency, Total Runs, Last Run)
 * - Visual interactive Activity Bar Chart (hover tooltips, click-to-inspect)
 * - Filterable Run History (All, Passed, Failed)
 * - Per-Request Diagnostic Breakdown with Method Badges, Resolved URLs, Status Pills, and Error Traces
 * - Live Run Now button & Auto-Refresh scheduler
 */
public class MonitorDetailDialog {

    public static void show(MonitorResponse monitor,
                            Window ownerWindow,
                            Consumer<MonitorResponse> onUpdated,
                            Runnable onDeleted) {

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Monitor Dashboard — " + monitor.getName());
        dialog.setResizable(true);
        if (ownerWindow != null) {
            dialog.initOwner(ownerWindow);
        }

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().add(ButtonType.CLOSE);
        ThemeManager.styleDialog(pane);

        dialog.setWidth(1080);
        dialog.setHeight(750);
        pane.setPrefSize(1080, 750);
        pane.setMinWidth(880);
        pane.setMinHeight(600);

        dialog.setOnShowing(e -> {
            Window win = pane.getScene() != null ? pane.getScene().getWindow() : null;
            if (win instanceof javafx.stage.Stage stage) {
                stage.setWidth(1080);
                stage.setHeight(750);
                stage.setMinWidth(880);
                stage.setMinHeight(600);
                stage.centerOnScreen();
            }
        });

        dialog.setOnShown(e -> {
            Window win = pane.getScene() != null ? pane.getScene().getWindow() : null;
            if (win instanceof javafx.stage.Stage stage) {
                stage.setWidth(1080);
                stage.setHeight(750);
                stage.setMinWidth(880);
                stage.setMinHeight(600);
                stage.centerOnScreen();
            }
        });

        final MonitorResponse[] currentMonitor = {monitor};
        final ObservableList<MonitorRunResponse> allRuns = FXCollections.observableArrayList();
        final FilteredList<MonitorRunResponse> filteredRuns = new FilteredList<>(allRuns, r -> true);

        // Header Elements
        Label iconLabel = new Label("⏱");
        iconLabel.setStyle("-fx-font-size: 26px; -fx-text-fill: -accent;");

        Label nameLabel = new Label(monitor.getName());
        nameLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: -text-primary;");

        Label statusBadge = new Label();
        statusBadge.getStyleClass().add("monitor-status-pill");

        HBox titleBox = new HBox(10, nameLabel, statusBadge);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        VBox headerInfo = new VBox(2, titleBox);

        // Action Buttons
        Button runNowBtn = new Button("▶  Run Now");
        runNowBtn.getStyleClass().add("primary-btn");
        runNowBtn.setMinWidth(Region.USE_PREF_SIZE);

        Button toggleStatusBtn = new Button(monitor.isEnabled() ? "⏸ Pause" : "▶ Resume");
        toggleStatusBtn.setMinWidth(Region.USE_PREF_SIZE);

        Button editBtn = new Button("✏ Edit");
        editBtn.setMinWidth(Region.USE_PREF_SIZE);

        Button deleteBtn = new Button("🗑 Delete");
        deleteBtn.getStyleClass().add("danger-menu-item");
        deleteBtn.setMinWidth(Region.USE_PREF_SIZE);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        HBox headerBar = new HBox(10, iconLabel, headerInfo, headerSpacer, runNowBtn, toggleStatusBtn, editBtn, deleteBtn);
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.setPadding(new Insets(4, 0, 8, 0));

        // Metadata Strip
        Label collectionMeta = new Label("📁 Collection: " + monitor.getCollectionName());
        collectionMeta.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: -text-secondary;");

        String envText = monitor.getEnvironmentName() != null && !monitor.getEnvironmentName().isBlank()
                ? "🌐 Env: " + monitor.getEnvironmentName() : "🌐 Env: (None)";
        Label envMeta = new Label(envText);
        envMeta.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-muted;");

        Label cadenceMeta = new Label("⏱ Schedule: Every " + monitor.getIntervalMinutes() + " mins");
        cadenceMeta.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-muted;");

        String alertText = monitor.isNotifyOnFailure()
                ? "🔔 Failure Alerts: Enabled (" + (monitor.getNotifyEmail() != null && !monitor.getNotifyEmail().isBlank() ? monitor.getNotifyEmail() : "default email") + ")"
                : "🔔 Failure Alerts: Disabled";
        Label alertMeta = new Label(alertText);
        alertMeta.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-muted;");

        Region metaSpacer = new Region();
        HBox.setHgrow(metaSpacer, Priority.ALWAYS);

        Label autoRefreshLabel = new Label("Live Refresh: Active");
        autoRefreshLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #10b981;");

        HBox metaStrip = new HBox(16, collectionMeta, envMeta, cadenceMeta, alertMeta, metaSpacer, autoRefreshLabel);
        metaStrip.getStyleClass().add("monitor-meta-strip");
        metaStrip.setAlignment(Pos.CENTER_LEFT);

        // KPI Performance Cards
        Label successRateVal = new Label("—");
        successRateVal.getStyleClass().add("monitor-kpi-value");
        Label successRateSub = new Label("Passing runs");
        successRateSub.getStyleClass().add("monitor-kpi-sub");
        VBox cardSuccess = new VBox(4, createKpiTitle("SUCCESS RATE"), successRateVal, successRateSub);
        cardSuccess.getStyleClass().add("monitor-kpi-card");
        HBox.setHgrow(cardSuccess, Priority.ALWAYS);

        Label avgLatencyVal = new Label("—");
        avgLatencyVal.getStyleClass().add("monitor-kpi-value");
        Label avgLatencySub = new Label("Per collection run");
        avgLatencySub.getStyleClass().add("monitor-kpi-sub");
        VBox cardLatency = new VBox(4, createKpiTitle("AVG LATENCY"), avgLatencyVal, avgLatencySub);
        cardLatency.getStyleClass().add("monitor-kpi-card");
        HBox.setHgrow(cardLatency, Priority.ALWAYS);

        Label totalRunsVal = new Label("0");
        totalRunsVal.getStyleClass().add("monitor-kpi-value");
        Label totalRunsSub = new Label("Total executed");
        totalRunsSub.getStyleClass().add("monitor-kpi-sub");
        VBox cardTotal = new VBox(4, createKpiTitle("RECORDED RUNS"), totalRunsVal, totalRunsSub);
        cardTotal.getStyleClass().add("monitor-kpi-card");
        HBox.setHgrow(cardTotal, Priority.ALWAYS);

        Label lastRunVal = new Label("—");
        lastRunVal.getStyleClass().add("monitor-kpi-value");
        Label lastRunSub = new Label("Latest run");
        lastRunSub.getStyleClass().add("monitor-kpi-sub");
        VBox cardLastRun = new VBox(4, createKpiTitle("LAST RESULT"), lastRunVal, lastRunSub);
        cardLastRun.getStyleClass().add("monitor-kpi-card");
        HBox.setHgrow(cardLastRun, Priority.ALWAYS);

        HBox kpiContainer = new HBox(12, cardSuccess, cardLatency, cardTotal, cardLastRun);

        // Interactive Timeline Bar Chart Section
        Label timelineTitle = new Label("Run Activity Timeline (Last 25 Runs)");
        timelineTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: -text-primary;");

        Label timelineSubtitle = new Label("Hover to view duration, click a bar to inspect request execution details.");
        timelineSubtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");

        Region timelineSpacer = new Region();
        HBox.setHgrow(timelineSpacer, Priority.ALWAYS);

        HBox timelineHeader = new HBox(10, timelineTitle, timelineSpacer, timelineSubtitle);
        timelineHeader.setAlignment(Pos.CENTER_LEFT);

        HBox timelineBarsBox = new HBox(4);
        timelineBarsBox.setAlignment(Pos.BOTTOM_LEFT);
        timelineBarsBox.setPrefHeight(45);
        timelineBarsBox.setMinHeight(45);

        VBox timelineContainer = new VBox(8, timelineHeader, timelineBarsBox);
        timelineContainer.getStyleClass().add("monitor-timeline-container");

        // Split View: Left = Run History Table, Right = Request Breakdown Inspector
        TableView<MonitorRunResponse> runsTable = new TableView<>(filteredRuns);
        runsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        runsTable.setPlaceholder(new Label("No runs recorded yet. Click 'Run Now' to execute."));

        TableColumn<MonitorRunResponse, Boolean> colResult = new TableColumn<>("Result");
        colResult.setCellValueFactory(new PropertyValueFactory<>("success"));
        colResult.setPrefWidth(90);
        colResult.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean success, boolean empty) {
                super.updateItem(success, empty);
                if (empty || success == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label pill = new Label(success ? "● Passed" : "⚠ Failed");
                    pill.getStyleClass().add("monitor-status-pill");
                    pill.getStyleClass().add(success ? "monitor-status-healthy" : "monitor-status-failing");
                    setGraphic(pill);
                    setText(null);
                }
            }
        });

        TableColumn<MonitorRunResponse, String> colStarted = new TableColumn<>("Started At");
        colStarted.setCellValueFactory(new PropertyValueFactory<>("startedAt"));
        colStarted.setPrefWidth(140);
        colStarted.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null);
                } else {
                    setText(formatTimestamp(val));
                }
            }
        });

        TableColumn<MonitorRunResponse, String> colRequests = new TableColumn<>("Requests");
        colRequests.setPrefWidth(85);
        colRequests.setCellValueFactory(data -> {
            MonitorRunResponse r = data.getValue();
            return new javafx.beans.property.SimpleStringProperty(r.getPassedRequests() + " / " + r.getTotalRequests());
        });

        TableColumn<MonitorRunResponse, Number> colAvg = new TableColumn<>("Avg ms");
        colAvg.setCellValueFactory(new PropertyValueFactory<>("avgResponseTimeMs"));
        colAvg.setPrefWidth(75);
        colAvg.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null);
                } else {
                    setText(val.longValue() + " ms");
                }
            }
        });

        runsTable.getColumns().addAll(colResult, colStarted, colRequests, colAvg);

        // Filter Pills for Run History
        ToggleButton filterAll = new ToggleButton("All");
        filterAll.setSelected(true);
        filterAll.getStyleClass().add("sub-tab-btn");

        ToggleButton filterPassed = new ToggleButton("Passed");
        filterPassed.getStyleClass().add("sub-tab-btn");

        ToggleButton filterFailed = new ToggleButton("Failed");
        filterFailed.getStyleClass().add("sub-tab-btn");

        ToggleGroup filterGroup = new ToggleGroup();
        filterAll.setToggleGroup(filterGroup);
        filterPassed.setToggleGroup(filterGroup);
        filterFailed.setToggleGroup(filterGroup);

        filterGroup.selectedToggleProperty().addListener((obs, old, selected) -> {
            if (selected == null) {
                filterAll.setSelected(true);
                return;
            }
            if (selected == filterPassed) {
                filteredRuns.setPredicate(MonitorRunResponse::isSuccess);
            } else if (selected == filterFailed) {
                filteredRuns.setPredicate(r -> !r.isSuccess());
            } else {
                filteredRuns.setPredicate(r -> true);
            }
        });

        HBox filterBar = new HBox(6, filterAll, filterPassed, filterFailed);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(0, 0, 4, 0));

        VBox leftPane = new VBox(8, filterBar, runsTable);
        VBox.setVgrow(runsTable, Priority.ALWAYS);
        leftPane.setMinWidth(360);

        // Right Pane: Request Breakdown Inspector
        VBox rightPane = new VBox(10);
        rightPane.getStyleClass().add("mock-inspector-box");
        rightPane.setPadding(new Insets(12));
        VBox.setVgrow(rightPane, Priority.ALWAYS);

        // Placeholder for Right Pane
        Label inspectorPlaceholder = new Label("Select a run from the history table on the left to view request-by-request execution details.");
        inspectorPlaceholder.setStyle("-fx-text-fill: -text-muted; -fx-font-size: 13px;");
        inspectorPlaceholder.setWrapText(true);
        inspectorPlaceholder.setAlignment(Pos.CENTER);
        VBox emptyInspectorBox = new VBox(inspectorPlaceholder);
        emptyInspectorBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(emptyInspectorBox, Priority.ALWAYS);
        rightPane.getChildren().setAll(emptyInspectorBox);

        SplitPane splitPane = new SplitPane(leftPane, rightPane);
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setDividerPositions(0.42);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        // Top Status Line
        Label runStatusNotice = new Label();
        runStatusNotice.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-secondary;");

        VBox rootLayout = new VBox(10, headerBar, metaStrip, kpiContainer, timelineContainer, splitPane, runStatusNotice);
        rootLayout.setPadding(new Insets(12));
        rootLayout.setMinWidth(880);
        rootLayout.setPrefWidth(1060);
        pane.setContent(rootLayout);

        // UI Refresh Helper Methods
        Runnable updateStatusBadgeAndBtns = () -> {
            boolean active = currentMonitor[0].isEnabled();
            String lastRun = currentMonitor[0].getLastRunStatus();

            statusBadge.getStyleClass().removeAll("monitor-status-healthy", "monitor-status-failing", "monitor-status-paused", "monitor-status-idle");
            if (!active) {
                statusBadge.setText("⏸ Paused");
                statusBadge.getStyleClass().add("monitor-status-paused");
            } else if ("passed".equalsIgnoreCase(lastRun)) {
                statusBadge.setText("● Healthy");
                statusBadge.getStyleClass().add("monitor-status-healthy");
            } else if ("failed".equalsIgnoreCase(lastRun) || "error".equalsIgnoreCase(lastRun)) {
                statusBadge.setText("⚠ Failing");
                statusBadge.getStyleClass().add("monitor-status-failing");
            } else {
                statusBadge.setText("○ Idle");
                statusBadge.getStyleClass().add("monitor-status-idle");
            }

            toggleStatusBtn.setText(active ? "⏸ Pause" : "▶ Resume");
        };

        // Render Request Breakdown Inspector for a given run
        Consumer<MonitorRunResponse> renderRunDetails = (run) -> {
            if (run == null) {
                rightPane.getChildren().setAll(emptyInspectorBox);
                return;
            }

            Label runTitle = new Label("Run Execution Details");
            runTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: -text-primary;");

            Label runResultBadge = new Label(run.isSuccess() ? "● ALL PASSED" : "⚠ SOME REQUESTS FAILED");
            runResultBadge.getStyleClass().add("monitor-status-pill");
            runResultBadge.getStyleClass().add(run.isSuccess() ? "monitor-status-healthy" : "monitor-status-failing");

            HBox inspectorTitleRow = new HBox(10, runTitle, runResultBadge);
            inspectorTitleRow.setAlignment(Pos.CENTER_LEFT);

            String timeDesc = formatTimestamp(run.getStartedAt());
            Label runSummary = new Label("Started: " + timeDesc + "  ·  Total Requests: " + run.getTotalRequests()
                    + "  ·  Passed: " + run.getPassedRequests() + "  ·  Failed: " + run.getFailedRequests()
                    + "  ·  Avg Latency: " + run.getAvgResponseTimeMs() + " ms");
            runSummary.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");

            // Filter search for requests inside this run
            TextField reqSearchField = new TextField();
            reqSearchField.setPromptText("Filter requests by name, method, or URL...");

            VBox reqCardsBox = new VBox(8);

            // Parse details JSON
            List<JSONObject> requestList = new ArrayList<>();
            if (run.getDetails() != null && !run.getDetails().isBlank()) {
                try {
                    JSONArray arr = new JSONArray(run.getDetails());
                    for (int i = 0; i < arr.length(); i++) {
                        requestList.add(arr.getJSONObject(i));
                    }
                } catch (Exception ignored) {
                }
            }

            Runnable updateFilteredRequests = () -> {
                reqCardsBox.getChildren().clear();
                String query = reqSearchField.getText() == null ? "" : reqSearchField.getText().trim().toLowerCase();

                int displayedCount = 0;
                for (JSONObject item : requestList) {
                    String name = item.optString("name", "Unnamed Request");
                    String method = item.optString("method", "GET");
                    String rawUrl = item.optString("url", "");
                    String resolvedUrl = item.optString("resolvedUrl", rawUrl);
                    int statusCode = item.optInt("statusCode", 0);
                    long durationMs = item.optLong("durationMs", 0);
                    boolean success = item.optBoolean("success", statusCode > 0 && statusCode < 400);
                    String error = item.optString("error", "");

                    if (!query.isEmpty()) {
                        boolean match = name.toLowerCase().contains(query)
                                || method.toLowerCase().contains(query)
                                || resolvedUrl.toLowerCase().contains(query)
                                || error.toLowerCase().contains(query);
                        if (!match) {
                            continue;
                        }
                    }

                    displayedCount++;
                    Node reqCard = createRequestCard(name, method, resolvedUrl, statusCode, durationMs, success, error);
                    reqCardsBox.getChildren().add(reqCard);
                }

                if (displayedCount == 0) {
                    Label noReqs = new Label(requestList.isEmpty()
                            ? "No request details recorded for this run."
                            : "No requests match your filter.");
                    noReqs.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-muted; -fx-padding: 20;");
                    reqCardsBox.getChildren().add(noReqs);
                }
            };

            reqSearchField.textProperty().addListener((obs, old, text) -> updateFilteredRequests.run());
            updateFilteredRequests.run();

            ScrollPane reqScroll = new ScrollPane(reqCardsBox);
            reqScroll.setFitToWidth(true);
            reqScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
            VBox.setVgrow(reqScroll, Priority.ALWAYS);

            rightPane.getChildren().setAll(inspectorTitleRow, runSummary, reqSearchField, reqScroll);
        };

        runsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            renderRunDetails.accept(selected);
        });

        // Rebuild KPI metrics and Timeline Bar Chart from loaded runs
        Runnable refreshMetricsAndTimeline = () -> {
            int total = allRuns.size();
            filterAll.setText("All (" + total + ")");

            if (total == 0) {
                successRateVal.setText("—");
                successRateVal.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: -text-primary;");
                avgLatencyVal.setText("—");
                totalRunsVal.setText("0");
                lastRunVal.setText("No runs yet");
                lastRunVal.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -text-muted;");
                timelineBarsBox.getChildren().clear();
                filterPassed.setText("Passed (0)");
                filterFailed.setText("Failed (0)");
                return;
            }

            long passedRunsCount = allRuns.stream().filter(MonitorRunResponse::isSuccess).count();
            long failedRunsCount = total - passedRunsCount;

            filterPassed.setText("Passed (" + passedRunsCount + ")");
            filterFailed.setText("Failed (" + failedRunsCount + ")");

            double successRate = (double) passedRunsCount * 100.0 / total;
            successRateVal.setText(String.format(Locale.US, "%.1f%%", successRate));
            if (successRate >= 95.0) {
                successRateVal.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #10b981;");
            } else if (successRate >= 80.0) {
                successRateVal.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #f59e0b;");
            } else {
                successRateVal.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ef4444;");
            }
            successRateSub.setText(passedRunsCount + " of " + total + " passed");

            long totalLatency = 0;
            for (MonitorRunResponse r : allRuns) {
                totalLatency += r.getAvgResponseTimeMs();
            }
            long overallAvgLatency = totalLatency / total;
            avgLatencyVal.setText(overallAvgLatency + " ms");

            totalRunsVal.setText(String.valueOf(total));

            MonitorRunResponse latestRun = allRuns.get(0);
            if (latestRun.isSuccess()) {
                lastRunVal.setText("Passed (" + latestRun.getPassedRequests() + "/" + latestRun.getTotalRequests() + ")");
                lastRunVal.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #10b981;");
            } else {
                lastRunVal.setText("Failed (" + latestRun.getFailedRequests() + " errors)");
                lastRunVal.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #ef4444;");
            }
            lastRunSub.setText(formatTimestamp(latestRun.getStartedAt()));

            // Populate Timeline Bars
            timelineBarsBox.getChildren().clear();
            // Show up to the last 25 runs in chronological order (oldest to newest)
            List<MonitorRunResponse> chronological = new ArrayList<>(allRuns);
            if (chronological.size() > 25) {
                chronological = chronological.subList(0, 25);
            }
            Collections.reverse(chronological);

            long maxLatency = 1;
            for (MonitorRunResponse r : chronological) {
                if (r.getAvgResponseTimeMs() > maxLatency) {
                    maxLatency = r.getAvgResponseTimeMs();
                }
            }

            for (MonitorRunResponse run : chronological) {
                Region bar = new Region();
                HBox.setHgrow(bar, Priority.ALWAYS);
                bar.setMinWidth(8);
                bar.setMaxWidth(30);

                double heightRatio = Math.max(0.3, Math.min(1.0, (double) run.getAvgResponseTimeMs() / (double) maxLatency));
                bar.setPrefHeight(38 * heightRatio);

                boolean ok = run.isSuccess();
                String barColor = ok ? "#10b981" : "#ef4444";
                bar.setStyle("-fx-background-color: " + barColor + "; -fx-background-radius: 4px 4px 0 0; -fx-cursor: hand;");

                Tooltip tt = new Tooltip(
                        "Time: " + formatTimestamp(run.getStartedAt()) + "\n"
                                + "Result: " + (ok ? "Passed" : "Failed") + "\n"
                                + "Requests: " + run.getPassedRequests() + "/" + run.getTotalRequests() + " passed\n"
                                + "Avg Latency: " + run.getAvgResponseTimeMs() + " ms"
                );
                Tooltip.install(bar, tt);

                bar.setOnMouseClicked(e -> {
                    runsTable.getSelectionModel().select(run);
                    runsTable.scrollTo(run);
                });

                timelineBarsBox.getChildren().add(bar);
            }
        };

        // Fetch Runs from Backend
        Runnable refreshRuns = () -> new Thread(() -> {
            Optional<List<MonitorRunResponse>> runs = MonitorService.getRuns(currentMonitor[0].getId());
            Platform.runLater(() -> {
                runs.ifPresent(list -> {
                    MonitorRunResponse previouslySelected = runsTable.getSelectionModel().getSelectedItem();
                    allRuns.setAll(list);
                    refreshMetricsAndTimeline.run();

                    if (previouslySelected != null) {
                        for (MonitorRunResponse r : list) {
                            if (Objects.equals(r.getId(), previouslySelected.getId())) {
                                runsTable.getSelectionModel().select(r);
                                break;
                            }
                        }
                    } else if (!list.isEmpty() && runsTable.getSelectionModel().getSelectedItem() == null) {
                        runsTable.getSelectionModel().selectFirst();
                    }
                });
            });
        }).start();

        updateStatusBadgeAndBtns.run();
        refreshRuns.run();

        // 1. Run Now Action
        runNowBtn.setOnAction(e -> {
            runNowBtn.setDisable(true);
            runNowBtn.setText("Running...");
            runStatusNotice.setText("Executing collection against monitor schedule...");

            new Thread(() -> {
                try {
                    MonitorRunResponse run = MonitorService.runNow(currentMonitor[0].getId());
                    Platform.runLater(() -> {
                        runNowBtn.setDisable(false);
                        runNowBtn.setText("▶  Run Now");
                        runStatusNotice.setText("Run complete! (" + run.getPassedRequests() + " passed, " + run.getFailedRequests() + " failed)");

                        // Update current monitor status
                        currentMonitor[0].setLastRunStatus(run.isSuccess() ? "passed" : "failed");
                        currentMonitor[0].setLastRunAt(run.getStartedAt());
                        updateStatusBadgeAndBtns.run();

                        refreshRuns.run();
                        if (onUpdated != null) {
                            onUpdated.accept(currentMonitor[0]);
                        }
                    });
                } catch (IOException ex) {
                    Platform.runLater(() -> {
                        runNowBtn.setDisable(false);
                        runNowBtn.setText("▶  Run Now");
                        runStatusNotice.setText("Run failed: " + MonitorService.friendlyMessage(ex));
                    });
                }
            }).start();
        });

        // 2. Toggle Status (Pause / Resume)
        toggleStatusBtn.setOnAction(e -> {
            toggleStatusBtn.setDisable(true);
            new Thread(() -> {
                Optional<MonitorResponse> updated = MonitorService.toggleEnabled(currentMonitor[0].getId());
                Platform.runLater(() -> {
                    toggleStatusBtn.setDisable(false);
                    updated.ifPresent(m -> {
                        currentMonitor[0] = m;
                        updateStatusBadgeAndBtns.run();
                        if (onUpdated != null) {
                            onUpdated.accept(m);
                        }
                    });
                });
            }).start();
        });

        // 3. Edit Action
        editBtn.setOnAction(e -> {
            MonitorEditDialog.show(currentMonitor[0], pane.getScene().getWindow(), saved -> {
                currentMonitor[0] = saved;
                nameLabel.setText(saved.getName());
                collectionMeta.setText("📁 Collection: " + saved.getCollectionName());
                envMeta.setText(saved.getEnvironmentName() != null ? "🌐 Env: " + saved.getEnvironmentName() : "🌐 Env: (None)");
                cadenceMeta.setText("⏱ Schedule: Every " + saved.getIntervalMinutes() + " mins");
                updateStatusBadgeAndBtns.run();
                if (onUpdated != null) {
                    onUpdated.accept(saved);
                }
            });
        });

        // 4. Delete Action
        deleteBtn.setOnAction(e -> {
            if (!AlertUtils.showConfirmation("Delete Monitor",
                    "Are you sure you want to delete monitor \"" + currentMonitor[0].getName() + "\"? All run history and performance metrics will be permanently removed.")) {
                return;
            }
            new Thread(() -> {
                boolean ok = MonitorService.deleteMonitor(currentMonitor[0].getId());
                Platform.runLater(() -> {
                    if (ok) {
                        dialog.close();
                        if (onDeleted != null) {
                            onDeleted.run();
                        }
                    } else {
                        runStatusNotice.setText("Couldn't delete this monitor.");
                    }
                });
            }).start();
        });

        // Background Auto-Refresh Timeline (every 10 seconds)
        Timeline autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(10), ev -> {
            refreshRuns.run();
        }));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();

        dialog.setOnHidden(ev -> autoRefreshTimeline.stop());

        dialog.showAndWait();
    }

    private static Label createKpiTitle(String title) {
        Label l = new Label(title);
        l.getStyleClass().add("monitor-kpi-title");
        return l;
    }

    private static Node createRequestCard(String name, String method, String resolvedUrl,
                                          int statusCode, long durationMs, boolean success, String error) {

        Label methodBadge = new Label(method);
        methodBadge.setStyle(getMethodBadgeStyle(method));

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: -text-primary;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label latencyBadge = new Label(durationMs + " ms");
        latencyBadge.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");

        Label statusBadge = new Label(statusCode > 0 ? (statusCode + " " + (success ? "OK" : "Error")) : "Error");
        statusBadge.setStyle(getStatusCodeBadgeStyle(statusCode, success));

        HBox topRow = new HBox(8, methodBadge, nameLabel, spacer, latencyBadge, statusBadge);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // URL display with copy button
        Label urlLabel = new Label(resolvedUrl);
        urlLabel.setStyle("-fx-font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace; -fx-font-size: 11px; -fx-text-fill: -text-secondary;");
        urlLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(urlLabel, Priority.ALWAYS);

        Button copyUrlBtn = new Button("📋");
        copyUrlBtn.getStyleClass().add("tree-inline-btn");
        copyUrlBtn.setTooltip(new Tooltip("Copy URL"));
        copyUrlBtn.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(resolvedUrl);
            Clipboard.getSystemClipboard().setContent(content);
        });

        HBox urlRow = new HBox(6, urlLabel, copyUrlBtn);
        urlRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(6, topRow, urlRow);
        card.getStyleClass().add("monitor-req-card");

        // Error diagnostic box if request failed
        if (!success && error != null && !error.isBlank()) {
            Label diagIcon = new Label("⚠");
            diagIcon.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 13px;");

            Label diagMsg = new Label(error);
            diagMsg.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px; -fx-font-family: monospace;");
            diagMsg.setWrapText(true);
            HBox.setHgrow(diagMsg, Priority.ALWAYS);

            HBox diagBox = new HBox(6, diagIcon, diagMsg);
            diagBox.getStyleClass().add("monitor-diag-box");
            diagBox.setAlignment(Pos.CENTER_LEFT);

            card.getChildren().add(diagBox);
        }

        return card;
    }

    private static String getMethodBadgeStyle(String method) {
        String base = "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 6 2 6; -fx-background-radius: 4px; -fx-border-radius: 4px; ";
        if ("GET".equalsIgnoreCase(method)) {
            return base + "-fx-background-color: rgba(16, 185, 129, 0.15); -fx-text-fill: #10b981; -fx-border-color: rgba(16, 185, 129, 0.35);";
        } else if ("POST".equalsIgnoreCase(method)) {
            return base + "-fx-background-color: rgba(245, 158, 11, 0.15); -fx-text-fill: #f59e0b; -fx-border-color: rgba(245, 158, 11, 0.35);";
        } else if ("PUT".equalsIgnoreCase(method)) {
            return base + "-fx-background-color: rgba(59, 130, 246, 0.15); -fx-text-fill: #3b82f6; -fx-border-color: rgba(59, 130, 246, 0.35);";
        } else if ("PATCH".equalsIgnoreCase(method)) {
            return base + "-fx-background-color: rgba(168, 85, 247, 0.15); -fx-text-fill: #a855f7; -fx-border-color: rgba(168, 85, 247, 0.35);";
        } else if ("DELETE".equalsIgnoreCase(method)) {
            return base + "-fx-background-color: rgba(239, 68, 68, 0.15); -fx-text-fill: #ef4444; -fx-border-color: rgba(239, 68, 68, 0.35);";
        }
        return base + "-fx-background-color: rgba(148, 163, 184, 0.15); -fx-text-fill: #94a3b8; -fx-border-color: rgba(148, 163, 184, 0.35);";
    }

    private static String getStatusCodeBadgeStyle(int statusCode, boolean success) {
        String base = "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 2 8 2 8; -fx-background-radius: 10px; ";
        if (success && statusCode >= 200 && statusCode < 300) {
            return base + "-fx-background-color: rgba(16, 185, 129, 0.18); -fx-text-fill: #10b981; -fx-border-color: rgba(16, 185, 129, 0.4); -fx-border-radius: 10px;";
        } else if (statusCode >= 300 && statusCode < 400) {
            return base + "-fx-background-color: rgba(59, 130, 246, 0.18); -fx-text-fill: #3b82f6; -fx-border-color: rgba(59, 130, 246, 0.4); -fx-border-radius: 10px;";
        } else {
            return base + "-fx-background-color: rgba(239, 68, 68, 0.18); -fx-text-fill: #ef4444; -fx-border-color: rgba(239, 68, 68, 0.4); -fx-border-radius: 10px;";
        }
    }

    private static String formatTimestamp(String raw) {
        if (raw == null || raw.isBlank()) {
            return "—";
        }
        // Normalize ISO timestamp T to space and trim milliseconds if present
        String clean = raw.replace("T", " ");
        int dotIdx = clean.indexOf('.');
        if (dotIdx > 0) {
            clean = clean.substring(0, dotIdx);
        }
        return clean;
    }
}
