package com.roze.thundercall.ui.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small, dependency-free JSON parser + pretty-printer, used only for the
 * request body's Ctrl+Shift+F "format" shortcut.
 * <p>
 * Written by hand rather than reusing {@code org.json}'s JSONObject: that
 * class deliberately backs itself with a plain {@code HashMap} — its own
 * source says so, "to ensure that elements are unordered" — so printing a
 * JSONObject back out silently reshuffles field order on every format.
 * That would be a jarring surprise compared to how every mainstream JSON
 * formatter behaves, Postman's beautify included (being JS-based, it
 * naturally preserves insertion order): this parser keeps object keys and
 * array elements in exactly the order they were written.
 * <p>
 * Numbers are kept as their original literal text rather than round-
 * tripped through a {@code double}, so formatting never rewrites "1.50"
 * to "1.5", drops a "+" exponent sign, or loses precision on a big
 * integer. This is a plain JSON parser — comments are expected to already
 * be gone (see {@link JsonCommentStripper}) before text reaches it.
 */
public final class JsonPrettyPrinter {

    private static final String INDENT_UNIT = "  "; // 2 spaces, matching this app's existing JSON output everywhere else

    private JsonPrettyPrinter() {
    }

    /** @throws IllegalArgumentException if {@code json} isn't valid JSON */
    public static String prettyPrint(String json) {
        Parser parser = new Parser(json);
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.atEnd()) {
            throw new IllegalArgumentException("Unexpected trailing content at position " + parser.pos);
        }
        StringBuilder out = new StringBuilder();
        write(value, out, 0);
        return out.toString();
    }

    private static void write(Object value, StringBuilder out, int depth) {
        if (value instanceof Map<?, ?> map) {
            writeObject(map, out, depth);
        } else if (value instanceof List<?> list) {
            writeArray(list, out, depth);
        } else {
            writeScalar(value, out);
        }
    }

    private static void writeObject(Map<?, ?> map, StringBuilder out, int depth) {
        if (map.isEmpty()) {
            out.append("{}");
            return;
        }
        out.append("{\n");
        String indent = INDENT_UNIT.repeat(depth + 1);
        int i = 0;
        int n = map.size();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            out.append(indent).append('"').append(escape((String) e.getKey())).append("\": ");
            write(e.getValue(), out, depth + 1);
            if (++i < n) {
                out.append(',');
            }
            out.append('\n');
        }
        out.append(INDENT_UNIT.repeat(depth)).append('}');
    }

    private static void writeArray(List<?> list, StringBuilder out, int depth) {
        if (list.isEmpty()) {
            out.append("[]");
            return;
        }
        out.append("[\n");
        String indent = INDENT_UNIT.repeat(depth + 1);
        for (int i = 0; i < list.size(); i++) {
            out.append(indent);
            write(list.get(i), out, depth + 1);
            if (i < list.size() - 1) {
                out.append(',');
            }
            out.append('\n');
        }
        out.append(INDENT_UNIT.repeat(depth)).append(']');
    }

    private static void writeScalar(Object value, StringBuilder out) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String s) {
            out.append('"').append(escape(s)).append('"');
        } else {
            out.append(value); // Boolean, or a RawNumber holding its original literal text
        }
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    /** Minimal recursive-descent JSON parser — objects preserve insertion
     * order via {@code LinkedHashMap}. */
    private static final class Parser {
        final String s;
        int pos;

        Parser(String s) {
            this.s = s;
            this.pos = 0;
        }

        boolean atEnd() {
            return pos >= s.length();
        }

        void skipWhitespace() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
                pos++;
            }
        }

        char peek() {
            if (atEnd()) {
                throw new IllegalArgumentException("Unexpected end of input");
            }
            return s.charAt(pos);
        }

        void expect(char c) {
            if (atEnd() || s.charAt(pos) != c) {
                throw new IllegalArgumentException("Expected '" + c + "' at position " + pos);
            }
            pos++;
        }

        Object parseValue() {
            skipWhitespace();
            char c = peek();
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't', 'f' -> parseBoolean();
                case 'n' -> parseNull();
                default -> parseNumber();
            };
        }

        Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (!atEnd() && peek() == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                char c = peek();
                if (c == ',') {
                    pos++;
                    continue;
                }
                if (c == '}') {
                    pos++;
                    break;
                }
                throw new IllegalArgumentException("Expected ',' or '}' at position " + pos);
            }
            return map;
        }

        List<Object> parseArray() {
            expect('[');
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (!atEnd() && peek() == ']') {
                pos++;
                return list;
            }
            while (true) {
                Object value = parseValue();
                list.add(value);
                skipWhitespace();
                char c = peek();
                if (c == ',') {
                    pos++;
                    continue;
                }
                if (c == ']') {
                    pos++;
                    break;
                }
                throw new IllegalArgumentException("Expected ',' or ']' at position " + pos);
            }
            return list;
        }

        String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (atEnd()) {
                    throw new IllegalArgumentException("Unterminated string");
                }
                char c = s.charAt(pos++);
                if (c == '"') {
                    break;
                }
                if (c == '\\') {
                    if (atEnd()) {
                        throw new IllegalArgumentException("Unterminated escape sequence");
                    }
                    char esc = s.charAt(pos++);
                    switch (esc) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'n' -> sb.append('\n');
                        case 't' -> sb.append('\t');
                        case 'r' -> sb.append('\r');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> {
                            if (pos + 4 > s.length()) {
                                throw new IllegalArgumentException("Invalid \\u escape");
                            }
                            String hex = s.substring(pos, pos + 4);
                            sb.append((char) Integer.parseInt(hex, 16));
                            pos += 4;
                        }
                        default -> throw new IllegalArgumentException(
                                "Invalid escape '\\" + esc + "' at position " + (pos - 1));
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        Boolean parseBoolean() {
            if (s.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (s.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Invalid literal at position " + pos);
        }

        Object parseNull() {
            if (s.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new IllegalArgumentException("Invalid literal at position " + pos);
        }

        RawNumber parseNumber() {
            int start = pos;
            if (!atEnd() && s.charAt(pos) == '-') {
                pos++;
            }
            if (atEnd() || !Character.isDigit(s.charAt(pos))) {
                throw new IllegalArgumentException("Invalid number at position " + pos);
            }
            while (!atEnd() && Character.isDigit(s.charAt(pos))) {
                pos++;
            }
            if (!atEnd() && s.charAt(pos) == '.') {
                pos++;
                if (atEnd() || !Character.isDigit(s.charAt(pos))) {
                    throw new IllegalArgumentException("Invalid number at position " + pos);
                }
                while (!atEnd() && Character.isDigit(s.charAt(pos))) {
                    pos++;
                }
            }
            if (!atEnd() && (s.charAt(pos) == 'e' || s.charAt(pos) == 'E')) {
                pos++;
                if (!atEnd() && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) {
                    pos++;
                }
                if (atEnd() || !Character.isDigit(s.charAt(pos))) {
                    throw new IllegalArgumentException("Invalid number at position " + pos);
                }
                while (!atEnd() && Character.isDigit(s.charAt(pos))) {
                    pos++;
                }
            }
            return new RawNumber(s.substring(start, pos));
        }
    }

    /** A number kept as its exact original source text so formatting never
     * silently rewrites its value or representation. */
    private record RawNumber(String literal) {
        @Override
        public String toString() {
            return literal;
        }
    }
}
