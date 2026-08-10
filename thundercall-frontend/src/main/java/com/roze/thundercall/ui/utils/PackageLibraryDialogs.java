package com.roze.thundercall.ui.utils;

import com.roze.thundercall.ui.models.ScriptPackage;
import com.roze.thundercall.ui.services.PackageLibraryService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;
import java.util.function.Consumer;

/**
 * Postman's real "Package Library" dialogs for the Scripts editor's
 * right-click "Save to Package Library" — a two-pane "New Package" editor
 * (package browser on the left, name/summary/code on the right, with the
 * module.exports/pm.require boilerplate that updates live as you type the
 * name) and a small "Add script to an existing package" search-and-pick
 * modal. Postman's real feature is a paid, team-synced one, so this
 * reproduces the UI/flow against the local {@link PackageLibraryService}
 * rather than a real backend.
 */
public final class PackageLibraryDialogs {

    private PackageLibraryDialogs() {
    }

    /** The "New Package" two-pane editor (Postman's package-library +
     * package-editor screens combined into one window). */
    public static void showNewPackage(Window owner, String initialScript, Consumer<ScriptPackage> onCreated) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Package Library");

        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("root", "package-library-dialog");

        TextField nameField = new TextField();
        nameField.setPromptText("package-name");
        nameField.getStyleClass().add("package-name-field");
        HBox.setHgrow(nameField, Priority.ALWAYS);

        Hyperlink addSummaryLink = new Hyperlink("Add a summary");
        TextField summaryField = new TextField();
        summaryField.setPromptText("Summary");
        summaryField.setVisible(false);
        summaryField.setManaged(false);
        addSummaryLink.setOnAction(e -> {
            addSummaryLink.setVisible(false);
            addSummaryLink.setManaged(false);
            summaryField.setVisible(true);
            summaryField.setManaged(true);
            summaryField.requestFocus();
        });

        TextArea codeArea = new TextArea();
        codeArea.getStyleClass().add("package-code-area");
        codeArea.setWrapText(false);
        codeArea.setText(buildTemplate(initialScript, ""));
        nameField.textProperty().addListener((o, ov, nv) -> codeArea.setText(buildTemplate(initialScript, nv.trim())));

        Button createButton = new Button("Create");
        createButton.getStyleClass().add("popup-submit-button");
        createButton.setDisable(true);
        nameField.textProperty().addListener((o, ov, nv) -> createButton.setDisable(nv.trim().isEmpty()));

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("secondary-button");
        cancelButton.setOnAction(e -> stage.close());

        createButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                return;
            }
            ScriptPackage pkg = PackageLibraryService.createPackage(
                    name, summaryField.getText().trim(), codeArea.getText());
            onCreated.accept(pkg);
            stage.close();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox headerRow = new HBox(10, nameField, spacer, cancelButton, createButton);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(12));

        VBox right = new VBox(6, headerRow, addSummaryLink, summaryField, codeArea);
        VBox.setVgrow(codeArea, Priority.ALWAYS);
        right.setPadding(new Insets(0, 12, 12, 12));
        right.getStyleClass().add("package-editor-pane");

        root.setLeft(buildLibrarySidebar());
        root.setCenter(right);

        Scene scene = new Scene(root, 940, 480);
        copyStylesheets(owner, scene);
        stage.setScene(scene);
        stage.show();
    }

    /** The "Add script to an existing package" picker: search + list +
     * Cancel/Select. Selecting a package appends the script to it. */
    public static void showExistingPackagePicker(Window owner, Consumer<ScriptPackage> onSelected) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Package Library");

        Label title = new Label("Add script to an existing package");
        title.getStyleClass().add("popup-title");
        title.setWrapText(true);

        TextField searchField = new TextField();
        searchField.setPromptText("Find packages...");

        ObservableList<ScriptPackage> all = FXCollections.observableArrayList(PackageLibraryService.listPackages());
        FilteredList<ScriptPackage> filtered = new FilteredList<>(all, p -> true);
        searchField.textProperty().addListener((o, ov, nv) -> {
            String q = nv == null ? "" : nv.toLowerCase();
            filtered.setPredicate(p -> p.getName() != null && p.getName().toLowerCase().contains(q));
        });

        ListView<ScriptPackage> listView = new ListView<>(filtered);
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ScriptPackage item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        VBox.setVgrow(listView, Priority.ALWAYS);

        Button selectButton = new Button("Select");
        selectButton.getStyleClass().add("popup-submit-button");
        selectButton.setDisable(true);
        listView.getSelectionModel().selectedItemProperty()
                .addListener((o, ov, nv) -> selectButton.setDisable(nv == null));

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("secondary-button");
        cancelButton.setOnAction(e -> stage.close());
        selectButton.setOnAction(e -> {
            ScriptPackage selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                onSelected.accept(selected);
                stage.close();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttonRow = new HBox(8, spacer, cancelButton, selectButton);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(12, title, searchField, listView, buttonRow);
        root.getStyleClass().addAll("root", "package-picker-dialog");
        root.setPadding(new Insets(16));
        root.setPrefSize(360, 420);

        Scene scene = new Scene(root);
        copyStylesheets(owner, scene);
        stage.setScene(scene);
        stage.show();
    }

    /** The left-hand package browser shown alongside the New Package
     * editor — informational (matches Postman's layout); it isn't wired
     * to switch the editor between packages. */
    private static VBox buildLibrarySidebar() {
        Label header = new Label("Package Library");
        header.getStyleClass().add("popup-title");

        Button newPackageButton = new Button("+ New Package");
        newPackageButton.getStyleClass().add("comment-icon-button");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox headerRow = new HBox(header, spacer, newPackageButton);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("Find packages...");

        List<ScriptPackage> packages = PackageLibraryService.listPackages();
        VBox content;
        if (packages.isEmpty()) {
            Label icon = new Label("\uD83D\uDCE6");
            icon.setStyle("-fx-font-size: 40px;");
            Label emptyTitle = new Label("No packages in this library yet!");
            emptyTitle.setWrapText(true);
            emptyTitle.getStyleClass().add("popup-title");
            Label emptyBody = new Label("Packages help you reuse common scripts.");
            emptyBody.setWrapText(true);
            emptyBody.getStyleClass().add("popup-label");
            content = new VBox(10, icon, emptyTitle, emptyBody);
            content.setAlignment(Pos.CENTER);
            content.setPadding(new Insets(30, 10, 10, 10));
        } else {
            ListView<ScriptPackage> listView = new ListView<>(FXCollections.observableArrayList(packages));
            listView.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(ScriptPackage item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName());
                }
            });
            VBox.setVgrow(listView, Priority.ALWAYS);
            content = new VBox(listView);
        }

        VBox left = new VBox(10, headerRow, searchField, content);
        VBox.setVgrow(content, Priority.ALWAYS);
        left.setPadding(new Insets(12));
        left.setPrefWidth(260);
        left.getStyleClass().add("package-library-sidebar");
        return left;
    }

    /** Postman's boilerplate for a package script: the code being saved,
     * then the module.exports/pm.require guidance comments — the require
     * path updates live as the package name is typed. */
    private static String buildTemplate(String initialScript, String packageName) {
        String name = packageName == null || packageName.isEmpty() ? "<package_name>" : packageName;
        StringBuilder sb = new StringBuilder();
        if (initialScript != null && !initialScript.isEmpty()) {
            sb.append(initialScript).append("\n\n");
        }
        sb.append("// Use module.exports to export the functions that should be\n");
        sb.append("// available to use from this package.\n");
        sb.append("// module.exports = { <your_function> }\n\n");
        sb.append("// Once exported, use this statement in your scripts to use the\n");
        sb.append("// package.\n");
        sb.append("// const myPackage = pm.require('").append(name).append("')\n");
        return sb.toString();
    }

    private static void copyStylesheets(Window owner, Scene scene) {
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getRoot().getStylesheets());
        }
    }
}
