package com.roze.thundercall.ui.utils;

import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import org.fxmisc.richtext.CodeArea;

import java.util.*;
import java.util.function.IntFunction;

/**
 * Postman-style JSON code folding manager for RichTextFX CodeArea.
 * Provides:
 *  1. Collapsible/expandable block folding with SVG chevron icons (∨ expanded, › collapsed).
 *  2. Dynamic line numbers in the gutter that skip hidden lines when collapsed.
 *  3. Preserves full un-folded JSON text for copying and saving.
 *  4. Clean gutter layout with right-aligned line numbers and vertical dividing line.
 *  5. Preserves scroll position across fold/unfold operations.
 */
public final class JsonFoldManager {

    public static class Block {
        public final int startLine; // 1-based original line index
        public final int endLine;   // 1-based original line index
        public final int indent;    // leading space count
        public final boolean isObject; // true for {}, false for []

        public Block(int startLine, int endLine, int indent, boolean isObject) {
            this.startLine = startLine;
            this.endLine = endLine;
            this.indent = indent;
            this.isObject = isObject;
        }

        @Override
        public String toString() {
            return (isObject ? "{" : "[") + startLine + ".." + endLine + "}";
        }
    }

    private static class BlockStart {
        final int lineNum;
        final char bracket;
        final int indent;

        BlockStart(int lineNum, char bracket, int indent) {
            this.lineNum = lineNum;
            this.bracket = bracket;
            this.indent = indent;
        }
    }

    public static class VisibleParagraph {
        public final int originalLineNum;
        public final String text;
        public final boolean isBlockStart;
        public final boolean isFolded;
        public final Block block;

        public VisibleParagraph(int originalLineNum, String text, boolean isBlockStart, boolean isFolded, Block block) {
            this.originalLineNum = originalLineNum;
            this.text = text;
            this.isBlockStart = isBlockStart;
            this.isFolded = isFolded;
            this.block = block;
        }
    }

    private final CodeArea codeArea;
    private String originalFullText = "";
    private List<String> originalLines = new ArrayList<>();
    private final Map<Integer, Block> blockByStartLine = new HashMap<>();
    private final Set<Integer> foldedStarts = new TreeSet<>();
    private final List<VisibleParagraph> visibleParagraphs = new ArrayList<>();
    private boolean isUpdating = false;

    private JsonFoldManager(CodeArea codeArea) {
        this.codeArea = codeArea;
        setFullText(codeArea.getText() != null ? codeArea.getText() : "");

        // Double-clicking on a folded line in the editor expands it
        this.codeArea.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                int parIdx = codeArea.getCurrentParagraph();
                if (parIdx >= 0 && parIdx < visibleParagraphs.size()) {
                    VisibleParagraph vp = visibleParagraphs.get(parIdx);
                    if (vp.isFolded) {
                        toggleFold(vp.originalLineNum);
                        event.consume();
                    }
                }
            }
        });
    }

    public static JsonFoldManager attach(CodeArea codeArea) {
        return new JsonFoldManager(codeArea);
    }

    public synchronized void setFullText(String fullText) {
        if (fullText == null || fullText.isEmpty()) {
            this.originalFullText = "";
            this.originalLines = Collections.singletonList("");
        } else {
            this.originalFullText = fullText;
            this.originalLines = Arrays.asList(fullText.split("\n", -1));
        }
        reparseBlocks();
        this.foldedStarts.clear();
        applyFolds();
    }

    public String getFullText() {
        return originalFullText;
    }

    public boolean isUpdating() {
        return isUpdating;
    }

    public int getFoldedCount() {
        return foldedStarts.size();
    }

    public synchronized void toggleFold(int originalStartLine) {
        if (foldedStarts.contains(originalStartLine)) {
            foldedStarts.remove(originalStartLine);
        } else {
            foldedStarts.add(originalStartLine);
        }
        applyFolds();
    }

    public synchronized void foldAll() {
        foldedStarts.clear();
        foldedStarts.addAll(blockByStartLine.keySet());
        applyFolds();
    }

    public synchronized void unfoldAll() {
        foldedStarts.clear();
        applyFolds();
    }

    private void reparseBlocks() {
        blockByStartLine.clear();
        Deque<BlockStart> stack = new ArrayDeque<>();

        for (int lineNum = 1; lineNum <= originalLines.size(); lineNum++) {
            String line = originalLines.get(lineNum - 1);
            boolean inString = false;
            boolean escape = false;
            int lineIndent = 0;
            while (lineIndent < line.length() && line.charAt(lineIndent) == ' ') {
                lineIndent++;
            }

            for (int c = 0; c < line.length(); c++) {
                char ch = line.charAt(c);
                if (escape) {
                    escape = false;
                    continue;
                }
                if (ch == '\\' && inString) {
                    escape = true;
                    continue;
                }
                if (ch == '"') {
                    inString = !inString;
                    continue;
                }
                if (inString) continue;

                // Single line comment
                if (ch == '/' && c + 1 < line.length() && line.charAt(c + 1) == '/') {
                    break;
                }

                if (ch == '{' || ch == '[') {
                    stack.push(new BlockStart(lineNum, ch, lineIndent));
                } else if (ch == '}' || ch == ']') {
                    char expected = (ch == '}') ? '{' : '[';
                    if (!stack.isEmpty() && stack.peek().bracket == expected) {
                        BlockStart start = stack.pop();
                        if (lineNum > start.lineNum) {
                            Block block = new Block(start.lineNum, lineNum, start.indent, expected == '{');
                            blockByStartLine.put(start.lineNum, block);
                        }
                    }
                }
            }
        }
    }

    private void applyFolds() {
        Set<Integer> hiddenLines = new HashSet<>();
        for (int startLine : foldedStarts) {
            Block b = blockByStartLine.get(startLine);
            if (b != null) {
                for (int l = b.startLine + 1; l < b.endLine; l++) {
                    hiddenLines.add(l);
                }
            }
        }

        visibleParagraphs.clear();
        List<String> visibleTexts = new ArrayList<>();

        for (int lineNum = 1; lineNum <= originalLines.size(); lineNum++) {
            if (hiddenLines.contains(lineNum)) {
                continue;
            }
            String lineText = originalLines.get(lineNum - 1);
            Block b = blockByStartLine.get(lineNum);
            boolean isBlockStart = (b != null);
            boolean isFolded = isBlockStart && foldedStarts.contains(lineNum);
            if (isFolded) {
                lineText = lineText + " ...";
            }
            visibleParagraphs.add(new VisibleParagraph(lineNum, lineText, isBlockStart, isFolded, b));
            visibleTexts.add(lineText);
        }

        String combined = String.join("\n", visibleTexts);
        double scrollY = codeArea.getEstimatedScrollY();
        int caret = Math.min(codeArea.getCaretPosition(), combined.length());

        isUpdating = true;
        try {
            codeArea.replaceText(combined);
            codeArea.setStyleSpans(0, JsonSyntaxHighlighter.computeHighlighting(combined));
            if (caret >= 0 && caret <= codeArea.getLength()) {
                codeArea.displaceCaret(caret);
            }
            codeArea.scrollYToPixel(scrollY);
        } finally {
            isUpdating = false;
        }

        // Re-set graphic factory so all line numbers and chevrons redraw cleanly
        codeArea.setParagraphGraphicFactory(getGutterFactory());
    }

    public IntFunction<Node> getGutterFactory() {
        return parIdx -> {
            if (parIdx < 0 || parIdx >= visibleParagraphs.size()) {
                return new Region();
            }
            VisibleParagraph vp = visibleParagraphs.get(parIdx);

            HBox row = new HBox(4);
            row.setAlignment(Pos.CENTER_RIGHT);
            row.getStyleClass().add("gutter-row");

            int digits = Math.max(2, String.valueOf(originalLines.size()).length());
            double minNumWidth = digits * 8.5 + 4.0;

            Label numLabel = new Label(String.valueOf(vp.originalLineNum));
            numLabel.getStyleClass().add("lineno");
            numLabel.setAlignment(Pos.CENTER_RIGHT);
            numLabel.setMinWidth(minNumWidth);
            numLabel.setPrefWidth(minNumWidth);

            StackPane arrowPane = new StackPane();
            arrowPane.setMinWidth(16);
            arrowPane.setPrefWidth(16);
            arrowPane.setMaxWidth(16);
            arrowPane.setAlignment(Pos.CENTER);

            if (vp.isBlockStart) {
                SVGPath chevron = new SVGPath();
                if (vp.isFolded) {
                    chevron.setContent("M 5 3 L 9 7 L 5 11");
                    chevron.getStyleClass().add("fold-chevron-collapsed");
                } else {
                    chevron.setContent("M 3 5 L 7 9 L 11 5");
                    chevron.getStyleClass().add("fold-chevron-expanded");
                }
                chevron.setStrokeWidth(1.6);
                chevron.setFill(Color.TRANSPARENT);

                arrowPane.getChildren().add(chevron);
                arrowPane.getStyleClass().add("fold-arrow-btn");
                arrowPane.setCursor(Cursor.HAND);
                Tooltip.install(arrowPane, new Tooltip(vp.isFolded ? "Expand" : "Collapse"));

                final int startLine = vp.originalLineNum;
                arrowPane.setOnMouseClicked(event -> {
                    toggleFold(startLine);
                    event.consume();
                });
                row.setCursor(Cursor.HAND);
                row.setOnMouseClicked(event -> {
                    toggleFold(startLine);
                    event.consume();
                });
            }

            row.getChildren().addAll(numLabel, arrowPane);
            return row;
        };
    }
}
