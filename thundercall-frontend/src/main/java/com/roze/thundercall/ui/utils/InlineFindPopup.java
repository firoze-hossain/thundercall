package com.roze.thundercall.ui.utils;

import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Popup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Small floating "Find" popup opened from the body/scripts editors'
 * right-click "Find: ..." menu item (see {@link EditorContextMenu}) —
 * case-insensitive substring search with next/previous navigation, styled
 * to match the existing response-viewer find bar ({@code CodeAreaSearch}).
 * Works against any {@link TextEditTarget}, so the same popup serves both
 * the CodeArea body editor and the plain-TextArea script editors.
 */
public final class InlineFindPopup {

    private final TextEditTarget target;
    private final Popup popup = new Popup();
    private final TextField searchField = new TextField();
    private final Label countLabel = new Label("0 of 0");
    private final List<int[]> matches = new ArrayList<>();
    private int currentMatch = -1;

    private InlineFindPopup(TextEditTarget target) {
        this.target = target;

        searchField.setPromptText("Find");
        searchField.getStyleClass().add("search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        countLabel.getStyleClass().add("search-count-label");

        Button prevBtn = new Button("\u2191");
        prevBtn.getStyleClass().add("search-nav-btn");
        prevBtn.setOnAction(e -> jump(-1));

        Button nextBtn = new Button("\u2193");
        nextBtn.getStyleClass().add("search-nav-btn");
        nextBtn.setOnAction(e -> jump(1));

        Button closeBtn = new Button("\u2715");
        closeBtn.getStyleClass().add("search-close-btn");
        closeBtn.setOnAction(e -> popup.hide());

        HBox bar = new HBox(6, searchField, countLabel, prevBtn, nextBtn, new Region(), closeBtn);
        bar.getStyleClass().addAll("root", "search-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6));
        bar.setPrefWidth(340);
        if (target.getNode().getScene() != null) {
            bar.getStylesheets().addAll(target.getNode().getScene().getRoot().getStylesheets());
        }

        searchField.textProperty().addListener((o, ov, nv) -> recompute());
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                jump(e.isShiftDown() ? -1 : 1);
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                popup.hide();
                e.consume();
            }
        });

        popup.getContent().add(bar);
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);
    }

    /** Opens (or re-opens) the find popup anchored near {@code target}'s
     * editor, pre-filled with {@code initialQuery} (typically the text that
     * was selected when "Find: ..." was chosen from the context menu). */
    public static void showFor(TextEditTarget target, String initialQuery) {
        InlineFindPopup finder = new InlineFindPopup(target);
        finder.searchField.setText(initialQuery == null ? "" : initialQuery);

        Node node = target.getNode();
        Bounds bounds = node.localToScreen(node.getBoundsInLocal());
        double x = bounds != null ? bounds.getMinX() + 16 : 120;
        double y = bounds != null ? bounds.getMinY() + 16 : 120;

        finder.popup.show(node, x, y);
        finder.searchField.requestFocus();
        finder.searchField.selectAll();
        finder.recompute();
    }

    private void recompute() {
        matches.clear();
        currentMatch = -1;
        String query = searchField.getText();
        String haystack = target.getText();
        if (query != null && !query.isEmpty() && haystack != null && !haystack.isEmpty()) {
            String hay = haystack.toLowerCase(Locale.ROOT);
            String needle = query.toLowerCase(Locale.ROOT);
            int from = 0;
            while (true) {
                int idx = hay.indexOf(needle, from);
                if (idx < 0) {
                    break;
                }
                matches.add(new int[]{idx, idx + needle.length()});
                from = idx + Math.max(needle.length(), 1);
            }
            if (!matches.isEmpty()) {
                currentMatch = 0;
            }
        }
        updateLabelAndSelection();
    }

    private void jump(int direction) {
        if (matches.isEmpty()) {
            return;
        }
        currentMatch = ((currentMatch + direction) % matches.size() + matches.size()) % matches.size();
        updateLabelAndSelection();
    }

    private void updateLabelAndSelection() {
        countLabel.setText(matches.isEmpty() ? "0 of 0" : (currentMatch + 1) + " of " + matches.size());
        if (currentMatch >= 0) {
            int[] m = matches.get(currentMatch);
            target.selectRange(m[0], m[1]);
        }
    }
}
