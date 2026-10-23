package com.roze.thundercall.ui.dialogs;

import com.roze.thundercall.ui.Main;
import com.roze.thundercall.ui.update.AppUpdateDialog;
import com.roze.thundercall.ui.update.AppUpdateService;
import com.roze.thundercall.ui.utils.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.Desktop;
import java.net.URI;

/**
 * Modern, professional About ThunderCall dialog.
 * Presents application branding, system environment diagnostics, feature overview,
 * and one-click navigation to documentation, support center, and updates.
 */
public final class AboutDialog extends Stage {

    private static final String DOCS_URL = "http://localhost:8000/docs/thundercall";
    private static final String SUPPORT_URL = "http://localhost:8000/support/thundercall";
    private static final String GITHUB_URL = "https://github.com/firoze-hossain/thundercall";

    public static void show(Window owner) {
        AboutDialog dialog = new AboutDialog(owner);
        dialog.show();
    }

    public AboutDialog(Window owner) {
        if (owner != null) {
            initOwner(owner);
        }
        initModality(Modality.APPLICATION_MODAL);
        setTitle("About ThunderCall");
        setResizable(false);

        try {
            getIcons().add(new Image(AboutDialog.class.getResourceAsStream("/images/logo.png")));
        } catch (Exception ignored) {
        }

        VBox root = new VBox(0);
        root.setPrefWidth(620);
        root.setMinWidth(620);
        root.setMaxWidth(620);
        root.setStyle("-fx-background-color: -bg-base;");

        // Top Hero Header
        VBox heroBox = buildHeroHeader();

        // Main Tab Content (Overview, System Info, Credits)
        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("settings-tab-pane");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        Tab featuresTab = new Tab("Features & Capabilities", buildFeaturesView());
        Tab systemTab = new Tab("System Diagnostics", buildSystemView());
        Tab creditsTab = new Tab("Credits & Ecosystem", buildCreditsView());

        tabPane.getTabs().addAll(featuresTab, systemTab, creditsTab);

        // Bottom Actions Bar
        HBox bottomBar = buildBottomBar();

        root.getChildren().addAll(heroBox, tabPane, bottomBar);

        Scene scene = new Scene(root);
        ThemeManager.apply(scene);
        setScene(scene);
    }

    private VBox buildHeroHeader() {
        VBox hero = new VBox(10);
        hero.setPadding(new Insets(24, 28, 18, 28));
        hero.setStyle("-fx-background-color: -bg-panel; -fx-border-color: -border-color; -fx-border-width: 0 0 1 0;");

        HBox brandRow = new HBox(16);
        brandRow.setAlignment(Pos.CENTER_LEFT);

        ImageView logoView = new ImageView();
        try {
            Image logoImg = new Image(AboutDialog.class.getResourceAsStream("/images/logo.png"));
            logoView.setImage(logoImg);
            logoView.setFitWidth(52);
            logoView.setFitHeight(52);
            logoView.setPreserveRatio(true);
        } catch (Exception ignored) {
        }

        VBox titleCol = new VBox(4);
        titleCol.setAlignment(Pos.CENTER_LEFT);

        HBox nameRow = new HBox(10);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label("ThunderCall");
        nameLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: -text-primary;");

        Label versionBadge = new Label("v" + AppUpdateService.CURRENT_VERSION);
        versionBadge.setStyle("-fx-background-color: -accent-soft; -fx-text-fill: -accent; " +
                "-fx-padding: 3 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold; -fx-border-color: -accent; -fx-border-radius: 12; -fx-border-width: 1;");

        Label channelBadge = new Label("STABLE");
        channelBadge.setStyle("-fx-background-color: rgba(16, 185, 129, 0.15); -fx-text-fill: #10b981; " +
                "-fx-padding: 3 8; -fx-background-radius: 12; -fx-font-size: 10px; -fx-font-weight: bold;");

        nameRow.getChildren().addAll(nameLabel, versionBadge, channelBadge);

        Label tagline = new Label("Next-Generation Calm, Fast & Powerful API Development Platform");
        tagline.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-secondary;");

        titleCol.getChildren().addAll(nameRow, tagline);
        brandRow.getChildren().addAll(logoView, titleCol);
        hero.getChildren().add(brandRow);

        return hero;
    }

    private Node buildFeaturesView() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(18, 24, 18, 24));

        box.getChildren().add(featureItem("⚡", "Multi-Protocol Support",
                "REST (HTTP/1.1 & HTTP/2), full-duplex WebSockets, Socket.IO, and GraphQL with live schema introspection."));
        box.getChildren().add(featureItem("📁", "Collections & Dynamic Variables",
                "Postman v2.1 import/export, nested folder hierarchies, and {{variable}} resolution across Global, Environment, and Collection scopes."));
        box.getChildren().add(featureItem("🧪", "Automated Script Assertions",
                "Pre-request and test scripts with real-time assertions, response time validation, and automatic variable chaining."));
        box.getChildren().add(featureItem("🎯", "Mock Servers & API Monitors",
                "Simulated route matching with latency controls and scheduled cron health monitors with uptime tracking."));
        box.getChildren().add(featureItem("👥", "Team Workspaces & Real-Time Presence",
                "Shared team workspaces, granular member roles (Owner, Admin, Editor, Viewer), and live online collaborator indicators."));

        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true);
        sp.setPrefHeight(230);
        sp.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return sp;
    }

    private HBox featureItem(String icon, String title, String desc) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.TOP_LEFT);
        item.setPadding(new Insets(8, 12, 8, 12));
        item.setStyle("-fx-background-color: -bg-panel; -fx-background-radius: 6; -fx-border-color: -border-subtle; -fx-border-radius: 6;");

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 18px;");

        VBox col = new VBox(2);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: -text-primary;");

        Label descLbl = new Label(desc);
        descLbl.setWrapText(true);
        descLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-secondary;");

        col.getChildren().addAll(titleLbl, descLbl);
        item.getChildren().addAll(iconLbl, col);
        HBox.setHgrow(col, Priority.ALWAYS);
        return item;
    }

    private Node buildSystemView() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(18, 24, 18, 24));

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(10);

        long usedMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
        long maxMem = Runtime.getRuntime().maxMemory() / (1024 * 1024);

        int row = 0;
        addSystemRow(grid, row++, "Application Version", "ThunderCall v" + AppUpdateService.CURRENT_VERSION + " (Build 2026.1)");
        addSystemRow(grid, row++, "Java Runtime", System.getProperty("java.runtime.version", System.getProperty("java.version")) + " (" + System.getProperty("java.vendor", "OpenJDK") + ")");
        addSystemRow(grid, row++, "UI Framework", "JavaFX 21 (OpenJFX) + RichTextFX Code Editor");
        addSystemRow(grid, row++, "Backend Engine", "Spring Boot 3.3.x / WebFlux Reactive Client");
        addSystemRow(grid, row++, "Operating System", System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ", " + System.getProperty("os.version") + ")");
        addSystemRow(grid, row++, "JVM Memory Allocated", usedMem + " MB used / " + maxMem + " MB max");
        addSystemRow(grid, row++, "Storage Directory", System.getProperty("user.home") + "/.thundercall");

        box.getChildren().add(grid);

        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true);
        sp.setPrefHeight(230);
        sp.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return sp;
    }

    private void addSystemRow(GridPane grid, int row, String label, String value) {
        Label l = new Label(label);
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: -text-secondary; -fx-font-size: 11px;");
        l.setMinWidth(150);

        Label v = new Label(value);
        v.setStyle("-fx-text-fill: -text-primary; -fx-font-size: 11px;");
        v.setWrapText(true);

        grid.add(l, 0, row);
        grid.add(v, 1, row);
    }

    private Node buildCreditsView() {
        VBox box = new VBox(14);
        box.setPadding(new Insets(20, 24, 20, 24));

        Label authorHeader = new Label("Author & Maintainer");
        authorHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: -text-primary;");

        Label authorName = new Label("Md. Firoze Hossain");
        authorName.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -accent;");

        Label ecoText = new Label("ThunderCall is part of the RozeHub developer toolchain ecosystem, designed to provide ultra-responsive, calm, and distraction-free developer experiences.");
        ecoText.setWrapText(true);
        ecoText.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-secondary; -fx-line-spacing: 3;");

        Label copyright = new Label("Copyright © 2026 ThunderCall Project. All rights reserved.");
        copyright.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");

        box.getChildren().addAll(authorHeader, authorName, ecoText, new Separator(), copyright);

        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true);
        sp.setPrefHeight(230);
        sp.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return sp;
    }

    private HBox buildBottomBar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPadding(new Insets(14, 20, 14, 20));
        bar.setStyle("-fx-background-color: -bg-panel; -fx-border-color: -border-color; -fx-border-width: 1 0 0 0;");

        Button docsBtn = new Button("📖 Documentation");
        docsBtn.getStyleClass().addAll("button", "button-secondary");
        docsBtn.setOnAction(e -> openBrowser(DOCS_URL));

        Button supportBtn = new Button("💬 Support Center");
        supportBtn.getStyleClass().addAll("button", "button-secondary");
        supportBtn.setOnAction(e -> openBrowser(SUPPORT_URL));

        Button updateBtn = new Button("Check for Updates…");
        updateBtn.getStyleClass().addAll("button", "button-secondary");
        updateBtn.setOnAction(e -> AppUpdateDialog.check(this, true));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().addAll("button", "button-primary");
        closeBtn.setOnAction(e -> close());

        bar.getChildren().addAll(docsBtn, supportBtn, updateBtn, spacer, closeBtn);
        return bar;
    }

    private void openBrowser(String url) {
        try {
            if (Main.getAppHostServices() != null) {
                Main.getAppHostServices().showDocument(url);
            } else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ex) {
            System.err.println("Failed to open browser URL " + url + ": " + ex.getMessage());
        }
    }
}
