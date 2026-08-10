package com.roze.thundercall.ui.utils;

import com.roze.thundercall.ui.models.CommentResponse;
import javafx.scene.input.MouseButton;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Layers a yellow "has a comment" highlight on top of a CodeArea's base
 * JSON syntax highlighting for every comment thread anchored to it — same
 * merge-spans technique as {@code CodeAreaSearch} — and reopens the right
 * thread when the user clicks inside a highlighted range.
 * <p>
 * Note: thread ranges are captured once, at comment-creation time, and
 * aren't re-mapped as the surrounding text is edited afterward — editing
 * inside/around a commented range can make its highlight drift, matching
 * the inherent limitation of a plain offset-based anchor.
 */
public final class CommentHighlighter {

    private final CodeArea area;
    private List<CommentResponse> threads = Collections.emptyList();

    public CommentHighlighter(CodeArea area, Consumer<CommentResponse> onThreadClicked) {
        this.area = area;
        area.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY || threads.isEmpty()) {
                return;
            }
            int pos = area.getCaretPosition();
            for (CommentResponse thread : threads) {
                if (thread.getRangeStart() != null && thread.getRangeEnd() != null
                        && pos >= thread.getRangeStart() && pos < thread.getRangeEnd()) {
                    onThreadClicked.accept(thread);
                    return;
                }
            }
        });
    }

    /** Replaces the set of comment threads highlighted in this editor and
     * repaints. Pass root threads only — replies share their root's range. */
    public void setThreads(List<CommentResponse> threads) {
        this.threads = threads != null ? threads : Collections.emptyList();
        refresh();
    }

    /** Recomputes JSON syntax highlighting plus the comment overlay — call
     * this on every text change, in place of a plain JsonSyntaxHighlighter
     * subscription. */
    public void refresh() {
        StyleSpans<Collection<String>> base = JsonSyntaxHighlighter.computeHighlighting(area.getText());
        area.setStyleSpans(0, merge(base, area.getText().length()));
    }

    private StyleSpans<Collection<String>> merge(StyleSpans<Collection<String>> base, int length) {
        List<Collection<String>> perChar = new ArrayList<>(length);
        for (var span : base) {
            for (int i = 0; i < span.getLength(); i++) {
                perChar.add(span.getStyle());
            }
        }
        while (perChar.size() < length) {
            perChar.add(Collections.emptyList());
        }
        for (CommentResponse thread : threads) {
            if (thread.getRangeStart() == null || thread.getRangeEnd() == null) {
                continue;
            }
            int start = Math.max(0, thread.getRangeStart());
            int end = Math.min(length, thread.getRangeEnd());
            for (int i = start; i < end; i++) {
                List<String> combined = new ArrayList<>(perChar.get(i));
                combined.add("comment-highlight");
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
