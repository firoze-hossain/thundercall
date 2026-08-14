package com.roze.thundercall.ui.utils;

import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Popup;

import java.util.List;

/**
 * Postman's "Set as new variable" card: Name / Value (read-only preview of
 * the selection) / Scope — Environment, Collection, Global, Vault, each
 * marked with its own colored letter icon exactly like Postman's picker.
 */
public final class SetVariablePopup {

    public interface Callback {
        void onSetVariable(String scope, String name, String value);
    }

    private static final List<String> SCOPES = List.of("Environment", "Collection", "Global", "Vault");

    private SetVariablePopup() {
    }

    public static void show(Node anchor, String selectedValue, Callback callback) {
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        Label title = new Label("Set as new variable");
        title.getStyleClass().add("popup-title");

        Label nameLabel = new Label("Name");
        nameLabel.getStyleClass().add("popup-label");
        TextField nameField = new TextField();
        nameField.setPromptText("Variable name");

        Label valueLabel = new Label("Value");
        valueLabel.getStyleClass().add("popup-label");
        TextField valueField = new TextField(flatten(selectedValue));
        valueField.setEditable(false);
        valueField.setFocusTraversable(false);

        Label scopeLabel = new Label("Scope");
        scopeLabel.getStyleClass().add("popup-label");
        ComboBox<String> scopeCombo = new ComboBox<>();
        scopeCombo.getItems().addAll(SCOPES);
        scopeCombo.setPromptText("Select a scope");
        scopeCombo.setMaxWidth(Double.MAX_VALUE);
        scopeCombo.setButtonCell(new ScopeCell());
        scopeCombo.setCellFactory(list -> new ScopeCell());

        Button setButton = new Button("Set Variable");
        setButton.getStyleClass().add("popup-submit-button");
        setButton.setMaxWidth(Double.MAX_VALUE);
        setButton.setDisable(true);

        Runnable updateEnabled = () -> setButton.setDisable(
                nameField.getText().trim().isEmpty() || scopeCombo.getValue() == null);
        nameField.textProperty().addListener((o, ov, nv) -> updateEnabled.run());
        scopeCombo.valueProperty().addListener((o, ov, nv) -> updateEnabled.run());

        setButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            String scope = scopeCombo.getValue();
            if (name.isEmpty() || scope == null) {
                return;
            }
            callback.onSetVariable(scope, name, selectedValue);
            popup.hide();
        });
        nameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER && !setButton.isDisable()) {
                setButton.fire();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                popup.hide();
            }
        });

        VBox box = new VBox(8, title,
                labeled(nameLabel, nameField),
                labeled(valueLabel, valueField),
                labeled(scopeLabel, scopeCombo),
                setButton);
        box.getStyleClass().addAll("root", "set-variable-popup");
        box.setPadding(new Insets(12));
        box.setPrefWidth(280);
        if (anchor.getScene() != null) {
            box.getStylesheets().addAll(anchor.getScene().getRoot().getStylesheets());
        }

        popup.getContent().add(box);

        Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        double x = bounds != null ? bounds.getMinX() + 24 : 150;
        double y = bounds != null ? bounds.getMinY() + 24 : 150;
        popup.show(anchor, x, y);
        PopupDismissal.closeOnOutsideClick(popup, anchor);
        nameField.requestFocus();
    }

    private static VBox labeled(Label label, Control control) {
        return new VBox(3, label, control);
    }

    private static String flatten(String s) {
        if (s == null) {
            return "";
        }
        String flat = s.replace("\n", " ").trim();
        return flat.length() > 60 ? flat.substring(0, 60) + "..." : flat;
    }

    /** Renders each scope with its Postman-style colored letter icon. */
    private static class ScopeCell extends ListCell<String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Circle dot = new Circle(7);
            String letter;
            switch (item) {
                case "Environment" -> {
                    dot.setFill(Color.web("#56c99f"));
                    letter = "E";
                }
                case "Collection" -> {
                    dot.setFill(Color.web("#d9a83e"));
                    letter = "C";
                }
                case "Global" -> {
                    dot.setFill(Color.web("#6aa8f0"));
                    letter = "G";
                }
                case "Vault" -> {
                    dot.setFill(Color.web("#e5726a"));
                    letter = "V";
                }
                default -> {
                    dot.setFill(Color.GRAY);
                    letter = "?";
                }
            }
            Label letterLabel = new Label(letter);
            letterLabel.setStyle("-fx-text-fill: white; -fx-font-size: 9px; -fx-font-weight: bold;");
            StackPane iconStack = new StackPane(dot, letterLabel);
            HBox row = new HBox(8, iconStack, new Label(item));
            row.setAlignment(Pos.CENTER_LEFT);
            setGraphic(row);
            setText(null);
        }
    }
}
