package com.roze.thundercall.ui.utils;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Postman-style coloring for the request body and response body editors:
 * JSON keys, string values, numbers, booleans/null, punctuation — plus
 * {{variable}} tokens, which always win regardless of where they appear
 * (inside a string, as a bare value, anywhere), matching the URL bar.
 *
 * This is intentionally a lightweight regex tokenizer, not a real parser —
 * it degrades gracefully on non-JSON or slightly malformed text instead of
 * throwing, which matters since the user is often mid-edit.
 */
public final class JsonSyntaxHighlighter {

    // {{...}} first (highest priority), then JSON tokens.
    //
    // FIX (critical): the KEY/STRING patterns used to be
    // "(?:\\.|[^"\\])*" — a GROUP wrapped in a "*" quantifier. Java's
    // regex engine matches that recursively, one stack frame per
    // repetition. A long JWT (800+ characters, no internal quotes)
    // caused hundreds of recursive calls and a real StackOverflowError
    // on the JavaFX thread. Restructured below so the bulk of any string
    // is consumed by a plain (non-recursive) character-class quantifier,
    // and the group only recurses once per ESCAPE SEQUENCE — which is
    // rare — instead of once per character.
    private static final Pattern PATTERN = Pattern.compile(
            "(?<VARIABLE>\\{\\{[^}]*}}?)"
                    + "|(?<KEY>\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"\\s*(?=:))"
                    + "|(?<STRING>\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\")"
                    + "|(?<NUMBER>-?\\b\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b)"
                    + "|(?<BOOLNULL>\\btrue\\b|\\bfalse\\b|\\bnull\\b)"
                    + "|(?<PUNCT>[{}\\[\\],:])"
    );

    private JsonSyntaxHighlighter() {
    }

    /** Above this, coloring risks noticeable lag on very large responses —
     * plain text is a better trade-off than a stall for a multi-MB body. */
    private static final int MAX_HIGHLIGHT_LENGTH = 400_000;

    public static StyleSpans<Collection<String>> computeHighlighting(String text) {
        if (text == null || text.isEmpty()) {
            return new StyleSpansBuilder<Collection<String>>().add(Collections.emptyList(), 0).create();
        }
        if (text.length() > MAX_HIGHLIGHT_LENGTH) {
            return new StyleSpansBuilder<Collection<String>>().add(Collections.emptyList(), text.length()).create();
        }
        Matcher matcher = PATTERN.matcher(text);
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        int lastEnd = 0;
        while (matcher.find()) {
            String styleClass =
                    matcher.group("VARIABLE") != null ? "json-variable"
                            : matcher.group("KEY") != null ? "json-key"
                            : matcher.group("STRING") != null ? "json-string"
                            : matcher.group("NUMBER") != null ? "json-number"
                            : matcher.group("BOOLNULL") != null ? "json-boolean"
                            : matcher.group("PUNCT") != null ? "json-punctuation"
                            : null;
            if (styleClass == null) {
                continue;
            }
            builder.add(Collections.emptyList(), matcher.start() - lastEnd);
            builder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastEnd = matcher.end();
        }
        builder.add(Collections.emptyList(), text.length() - lastEnd);
        return builder.create();
    }

    /** Single-line version for the URL bar: only {{variables}} are colored
     * (a URL isn't JSON, so key/string/number coloring would be noise). */
    private static final Pattern VARIABLE_ONLY = Pattern.compile("\\{\\{[^}]*}}?");

    public static StyleSpans<Collection<String>> computeUrlHighlighting(String text) {
        if (text == null || text.isEmpty()) {
            return new StyleSpansBuilder<Collection<String>>().add(Collections.emptyList(), 0).create();
        }
        Matcher matcher = VARIABLE_ONLY.matcher(text);
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        int lastEnd = 0;
        while (matcher.find()) {
            builder.add(Collections.emptyList(), matcher.start() - lastEnd);
            builder.add(Collections.singleton("json-variable"), matcher.end() - matcher.start());
            lastEnd = matcher.end();
        }
        builder.add(Collections.emptyList(), text.length() - lastEnd);
        return builder.create();
    }
}