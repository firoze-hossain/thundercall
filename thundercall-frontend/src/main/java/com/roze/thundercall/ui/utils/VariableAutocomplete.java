package com.roze.thundercall.ui.utils;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import org.fxmisc.richtext.CodeArea;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Attaches two Postman-style editing conveniences to a RichTextFX CodeArea:
 *
 *  1. Auto-closing braces — typing "{" inserts the matching "}" and leaves
 *     the caret between them (typing "{{" naturally becomes "{{}}" with
 *     the caret in the middle, exactly like real editors).
 *  2. {{variable}} autocomplete — the moment the caret sits inside an
 *     unfinished "{{...", a popup lists the current environment's
 *     variables (name + live value preview), filtered as you keep typing.
 *     Enter or a click completes it to "{{NAME}}".
 */
public final class VariableAutocomplete {

    private VariableAutocomplete() {
    }

    /** @param environmentVariables supplies the CURRENT environment's
     *  variables fresh every time the popup opens (so switching
     *  environments is reflected immediately). */
    public static void attach(CodeArea area, Supplier<Map<String, String>> environmentVariables) {
        Popup popup = new Popup();
        popup.setAutoHide(true);
        ListView<String> suggestions = new ListView<>();
        suggestions.setPrefWidth(320);
        suggestions.setMaxHeight(220);
        suggestions.getStyleClass().add("variable-suggestions");
        suggestions.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty || name == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Map<String, String> vars = environmentVariables.get();
                String value = vars != null ? vars.getOrDefault(name, "") : "";
                Label nameLabel = new Label(name);
                nameLabel.getStyleClass().add("variable-suggestion-name");
                Label valueLabel = new Label(value.length() > 40 ? value.substring(0, 37) + "..." : value);
                valueLabel.getStyleClass().add("variable-suggestion-value");
                VBox box = new VBox(1, nameLabel, valueLabel);
                setGraphic(box);
                setText(null);
            }
        });
        popup.getContent().add(suggestions);

        // Guards against re-entrant text-change events while we ourselves
        // are the ones inserting text (auto-close, autocomplete completion).
        boolean[] selfEdit = {false};

        area.plainTextChanges().subscribe(change -> {
            if (selfEdit[0]) {
                return;
            }
            String inserted = change.getInserted();
            if ("{".equals(inserted)) {
                int caret = area.getCaretPosition();
                String text = area.getText();
                boolean nextIsClosingBrace = caret < text.length() && text.charAt(caret) == '}';
                if (!nextIsClosingBrace) {
                    selfEdit[0] = true;
                    area.insertText(caret, "}");
                    area.moveTo(caret);
                    selfEdit[0] = false;
                }
            }
            updateSuggestionPopup(area, popup, suggestions, environmentVariables);
        });

        area.caretPositionProperty().addListener((obs, oldPos, newPos) ->
                updateSuggestionPopup(area, popup, suggestions, environmentVariables));

        area.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (!popup.isShowing()) {
                return;
            }
            if (event.getCode() == KeyCode.DOWN) {
                int i = suggestions.getSelectionModel().getSelectedIndex();
                if (i < suggestions.getItems().size() - 1) {
                    suggestions.getSelectionModel().select(i + 1);
                }
                event.consume();
            } else if (event.getCode() == KeyCode.UP) {
                int i = suggestions.getSelectionModel().getSelectedIndex();
                if (i > 0) {
                    suggestions.getSelectionModel().select(i - 1);
                }
                event.consume();
            } else if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.TAB) {
                String selected = suggestions.getSelectionModel().getSelectedItem();
                if (selected == null && !suggestions.getItems().isEmpty()) {
                    selected = suggestions.getItems().get(0);
                }
                if (selected != null) {
                    completeVariable(area, selfEdit, selected);
                }
                popup.hide();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                popup.hide();
                event.consume();
            }
        });

        suggestions.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                String selected = suggestions.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    completeVariable(area, selfEdit, selected);
                }
                popup.hide();
            }
        });
    }

    /** Finds an unfinished "{{partial" token ending at the caret, and
     * either shows a filtered popup or hides it if there's no such token. */
    private static void updateSuggestionPopup(CodeArea area, Popup popup, ListView<String> suggestions,
                                              Supplier<Map<String, String>> environmentVariables) {
        int caret = area.getCaretPosition();
        String text = area.getText();
        int searchFrom = Math.max(0, caret - 60); // a variable name is never absurdly long
        String before = text.substring(searchFrom, Math.min(caret, text.length()));
        int openIdx = before.lastIndexOf("{{");
        if (openIdx < 0) {
            popup.hide();
            return;
        }
        String partial = before.substring(openIdx + 2);
        // If the token was already closed before the caret, we're not "inside" it anymore
        if (partial.contains("}")) {
            popup.hide();
            return;
        }

        Map<String, String> vars = environmentVariables.get();
        if (vars == null || vars.isEmpty()) {
            popup.hide();
            return;
        }
        String filter = partial.trim().toLowerCase();
        var matches = vars.keySet().stream()
                .filter(name -> name.toLowerCase().contains(filter))
                .sorted()
                .collect(Collectors.toCollection(java.util.ArrayList::new));
        if (matches.isEmpty()) {
            popup.hide();
            return;
        }
        suggestions.getItems().setAll(matches);
        suggestions.getSelectionModel().select(0);

        if (!popup.isShowing()) {
            area.caretBoundsProperty().getValue().ifPresent(bounds -> showPopupNear(area, popup, bounds));
        } else {
            area.caretBoundsProperty().getValue().ifPresent(bounds -> {
                Point2D screen = area.localToScreen(bounds.getMinX(), bounds.getMaxY());
                if (screen != null) {
                    popup.setX(screen.getX());
                    popup.setY(screen.getY() + 4);
                }
            });
        }
    }

    private static void showPopupNear(CodeArea area, Popup popup, Bounds caretBounds) {
        Point2D screen = area.localToScreen(caretBounds.getMinX(), caretBounds.getMaxY());
        if (screen == null) {
            return;
        }
        popup.show(area, screen.getX(), screen.getY() + 4);
    }

    /** Replaces the unfinished "{{partial" token ending at the caret with
     * a completed "{{NAME}}". */
    private static void completeVariable(CodeArea area, boolean[] selfEdit, String variableName) {
        int caret = area.getCaretPosition();
        String text = area.getText();
        int searchFrom = Math.max(0, caret - 60);
        String before = text.substring(searchFrom, Math.min(caret, text.length()));
        int openIdx = before.lastIndexOf("{{");
        if (openIdx < 0) {
            return;
        }
        int tokenStart = searchFrom + openIdx;
        // Also consume an auto-inserted "}}" sitting right after the caret, if present
        int afterCaret = caret;
        if (text.startsWith("}}", afterCaret)) {
            afterCaret += 2;
        }
        selfEdit[0] = true;
        area.replaceText(tokenStart, afterCaret, "{{" + variableName + "}}");
        area.moveTo(tokenStart + variableName.length() + 4);
        selfEdit[0] = false;
    }
}