package com.roze.thundercall.ui.utils;

/**
 * Strips "//" line comments and "/* *&#47;" block comments from an
 * otherwise-JSON string, purely so the app's "does this look like valid
 * JSON" check can tolerate the kind of documentation comments and
 * commented-out fields Postman's raw body editor happily lets you leave in
 * (e.g. commenting out "fatherName"/"motherName" while filling in a form).
 * <p>
 * Comments are only ever recognized OUTSIDE of a string literal, so
 * "https://example.com" inside a JSON string value is never mistaken for
 * the start of a comment.
 * <p>
 * This is used ONLY to decide whether the body counts as valid JSON — the
 * raw text (comments and all) is still exactly what gets sent as the
 * request body, the same way Postman sends the editor's raw contents
 * verbatim rather than rewriting what you typed.
 */
public final class JsonCommentStripper {

    private JsonCommentStripper() {
    }

    public static String strip(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        boolean inString = false;
        boolean escaped = false;
        int i = 0;
        int n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (inString) {
                out.append(c);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                i++;
                continue;
            }
            if (c == '"') {
                inString = true;
                out.append(c);
                i++;
                continue;
            }
            if (c == '/' && i + 1 < n && text.charAt(i + 1) == '/') {
                i += 2;
                while (i < n && text.charAt(i) != '\n') {
                    i++;
                }
                continue; // the newline (if any) is re-processed and kept
            }
            if (c == '/' && i + 1 < n && text.charAt(i + 1) == '*') {
                i += 2;
                while (i < n && !(text.charAt(i) == '*' && i + 1 < n && text.charAt(i + 1) == '/')) {
                    if (text.charAt(i) == '\n') {
                        out.append('\n'); // preserve line breaks so any
                        // line-numbered parser error still points sensibly
                    }
                    i++;
                }
                i = Math.min(i + 2, n); // skip the closing "*/"
                continue;
            }
            out.append(c);
            i++;
        }
        return stripTrailingCommas(out.toString());
    }

    /** Removes a comma that ends up trailing — nothing but whitespace
     * before a "}" or "]" — the natural leftover once a comment-stripped
     * line was the last property/element in an object or array. */
    private static String stripTrailingCommas(String json) {
        return json.replaceAll(",(\\s*)([}\\]])", "$1$2");
    }

    /** True if {@code text} contains a "//" line comment or "/* *&#47;"
     * block comment OUTSIDE any string literal — unlike a plain substring
     * search, a "//" inside a URL or other string value doesn't count.
     * Used to decide whether to mention "(comments removed)" after a
     * format operation. */
    public static boolean containsComment(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        boolean inString = false;
        boolean escaped = false;
        int i = 0;
        int n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                i++;
                continue;
            }
            if (c == '"') {
                inString = true;
                i++;
                continue;
            }
            if (c == '/' && i + 1 < n && (text.charAt(i + 1) == '/' || text.charAt(i + 1) == '*')) {
                return true;
            }
            i++;
        }
        return false;
    }
}
