package com.roze.thundercall.ui.utils;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Ctrl/Cmd+F find bar for a read-only response viewer — case-insensitive
 * substring search with match count and next/previous navigation, matching
 * Postman's response search. Sits on top of the existing JSON/variable
 * syntax highlighting rather than replacing it.
 */
public final class CodeAreaSearch {

    private final CodeArea area;
    private final HBox bar;
    private final TextField searchField;
    private final Label countLabel;
    private final List<int[]> matches = new ArrayList<>();
    private int currentMatch = -1;
    private boolean active = false;

    private CodeAreaSearch(CodeArea area) {
        this.area = area;

        searchField = new TextField();
        searchField.setPromptText("Find in response");
        searchField.getStyleClass().add("search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        countLabel = new Label("0 of 0");
        countLabel.getStyleClass().add("search-count-label");

        Button prevBtn = new Button("\u2191");
        prevBtn.getStyleClass().add("search-nav-btn");
        prevBtn.setOnAction(e -> jump(-1));

        Button nextBtn = new Button("\u2193");
        nextBtn.getStyleClass().add("search-nav-btn");
        nextBtn.setOnAction(e -> jump(1));

        Button closeBtn = new Button("\u2715");
        closeBtn.getStyleClass().add("search-close-btn");
        closeBtn.setOnAction(e -> close());

        bar = new HBox(6, searchField, countLabel, prevBtn, nextBtn, new Region(), closeBtn);
        bar.getStyleClass().add("search-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setManaged(false);
        bar.setVisible(false);

        searchField.textProperty().addListener((o, ov, nv) -> recomputeMatches());
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                jump(e.isShiftDown() ? -1 : 1);
            } else if (e.getCode() == KeyCode.ESCAPE) {
                close();
            }
        });

        area.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            boolean shortcutDown = event.isControlDown() || event.isMetaDown();
            if (shortcutDown && event.getCode() == KeyCode.F) {
                open();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE && active) {
                close();
                event.consume();
            }
        });
    }

    /** Attaches a find bar to a CodeArea. Returns the bar's Node (initially
     * invisible/unmanaged) — add it into the layout above the CodeArea. */
    public static CodeAreaSearch attach(CodeArea area) {
        return new CodeAreaSearch(area);
    }

    public HBox getBar() {
        return bar;
    }

    public void open() {
        active = true;
        bar.setManaged(true);
        bar.setVisible(true);
        searchField.requestFocus();
        recomputeMatches();
    }

    public void close() {
        active = false;
        bar.setManaged(false);
        bar.setVisible(false);
        matches.clear();
        currentMatch = -1;
        applyHighlighting();
        area.requestFocus();
    }

    /** Call this whenever the response text is replaced, so a stale search
     * doesn't linger over content it no longer matches. */
    public void reset() {
        if (active) {
            close();
        }
    }

    private void recomputeMatches() {
        matches.clear();
        currentMatch = -1;
        String query = searchField.getText();
        if (query != null && !query.isEmpty()) {
            String text = area.getText();
            String haystack = text.toLowerCase(Locale.ROOT);
            String needle = query.toLowerCase(Locale.ROOT);
            int from = 0;
            while (true) {
                int idx = haystack.indexOf(needle, from);
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
        updateCountLabel();
        applyHighlighting();
        scrollToCurrent();
    }

    private void jump(int direction) {
        if (matches.isEmpty()) {
            return;
        }
        currentMatch = ((currentMatch + direction) % matches.size() + matches.size()) % matches.size();
        updateCountLabel();
        applyHighlighting();
        scrollToCurrent();
    }

    private void scrollToCurrent() {
        if (currentMatch >= 0 && currentMatch < matches.size()) {
            area.moveTo(matches.get(currentMatch)[0]);
            area.requestFollowCaret();
        }
    }

    private void updateCountLabel() {
        countLabel.setText(matches.isEmpty() ? "0 of 0" : (currentMatch + 1) + " of " + matches.size());
    }

    /** Rebuilds styling: base JSON/variable classification, with search
     * match highlighting layered on top (current match styled distinctly
     * from the other matches, exactly like a browser's find bar). */
    private void applyHighlighting() {
        StyleSpans<Collection<String>> base = JsonSyntaxHighlighter.computeHighlighting(area.getText());
        area.setStyleSpans(0, buildMergedSpans(base, area.getText().length()));
    }

    private StyleSpans<Collection<String>> buildMergedSpans(StyleSpans<Collection<String>> base, int length) {
        List<Collection<String>> perChar = new ArrayList<>(length);
        for (var span : base) {
            for (int i = 0; i < span.getLength(); i++) {
                perChar.add(span.getStyle());
            }
        }
        while (perChar.size() < length) {
            perChar.add(Collections.emptyList());
        }
        for (int mi = 0; mi < matches.size(); mi++) {
            int[] m = matches.get(mi);
            String matchClass = mi == currentMatch ? "search-match-current" : "search-match";
            for (int i = m[0]; i < m[1] && i < perChar.size(); i++) {
                List<String> combined = new ArrayList<>(perChar.get(i));
                combined.add(matchClass);
                perChar.set(i, combined);
            }
        }
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        if (perChar.isEmpty()) {
            builder.add(Collections.emptyList(), 0);
            return builder.create();
        }
        Collection<String> runStyle = perChar.get(0);
        int runLength = 0;
        for (Collection<String> style : perChar) {
            if (style.equals(runStyle)) {
                runLength++;
            } else {
                builder.add(runStyle, runLength);
                runStyle = style;
                runLength = 1;
            }
        }
        builder.add(runStyle, runLength);
        return builder.create();
    }
}