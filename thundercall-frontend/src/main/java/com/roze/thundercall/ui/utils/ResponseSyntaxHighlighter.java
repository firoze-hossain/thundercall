package com.roze.thundercall.ui.utils;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Multi-format syntax highlighter and formatter for Postman response view options:
 * JSON, XML, HTML, JavaScript, Raw, Hex, Base64.
 */
public final class ResponseSyntaxHighlighter {

    private static final Pattern XML_PATTERN = Pattern.compile(
            "(?<COMMENT><!--[\\s\\S]*?-->)"
            + "|(?<DECLARATION><\\?[xX][mM][lL][\\s\\S]*?\\?>)"
            + "|(?<TAG><\\/?[a-zA-Z0-9_\\-:]+)"
            + "|(?<CLOSETAG>\\/?>)"
            + "|(?<ATTR>[a-zA-Z0-9_\\-:]+)(?=\\s*=)"
            + "|(?<VALUE>\"[^\"]*\"|'[^']*')"
    );

    private static final Pattern HTML_PATTERN = Pattern.compile(
            "(?<COMMENT><!--[\\s\\S]*?-->)"
            + "|(?<DOCTYPE><![dD][oO][cC][tT][yY][pP][eE][^>]*>)"
            + "|(?<TAG><\\/?[a-zA-Z0-9_\\-:]+)"
            + "|(?<CLOSETAG>\\/?>)"
            + "|(?<ATTR>[a-zA-Z0-9_\\-:]+)(?=\\s*=)"
            + "|(?<VALUE>\"[^\"]*\"|'[^']*')"
    );

    private static final Pattern JS_PATTERN = Pattern.compile(
            "(?<COMMENT>//.*$|/\\*[\\s\\S]*?\\*/)"
            + "|(?<KEYWORD>\\b(?:true|false|null|undefined|function|return|var|let|const|if|else|for|while|new|this|class|import|export|async|await|try|catch)\\b)"
            + "|(?<STRING>\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'|`[^`\\\\]*(?:\\\\.[^`\\\\]*)*`)"
            + "|(?<NUMBER>\\b-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b)"
            , Pattern.MULTILINE
    );

    private static final Pattern HEX_PATTERN = Pattern.compile(
            "(?m)^(?<OFFSET>[0-9a-fA-F]{8}:)"
            + "(?<BYTES> (?:[0-9a-fA-F]{2} ?){1,16} *)"
            + "(?<GAP> {2,})"
            + "(?<ASCII>[^\\r\\n]*)"
    );

    private static final int MAX_HIGHLIGHT_LENGTH = 400_000;

    private ResponseSyntaxHighlighter() {}

    public static StyleSpans<Collection<String>> computeHighlighting(String text, String format) {
        if (text == null || text.isEmpty()) {
            return new StyleSpansBuilder<Collection<String>>()
                    .add(Collections.emptyList(), 0)
                    .create();
        }

        if (text.length() > MAX_HIGHLIGHT_LENGTH) {
            return new StyleSpansBuilder<Collection<String>>()
                    .add(Collections.emptyList(), text.length())
                    .create();
        }

        if (format == null) format = "JSON";

        return switch (format.toUpperCase()) {
            case "JSON" -> JsonSyntaxHighlighter.computeHighlighting(text);
            case "XML" -> computePatternHighlighting(text, XML_PATTERN, "xml");
            case "HTML" -> computePatternHighlighting(text, HTML_PATTERN, "html");
            case "JAVASCRIPT", "JS" -> computeJsHighlighting(text);
            case "HEX" -> computeHexHighlighting(text);
            case "BASE64" -> new StyleSpansBuilder<Collection<String>>()
                    .add(Collections.singleton("base64-text"), text.length())
                    .create();
            default -> new StyleSpansBuilder<Collection<String>>()
                    .add(Collections.emptyList(), text.length())
                    .create();
        };
    }

    private static StyleSpans<Collection<String>> computePatternHighlighting(String text, Pattern pattern, String prefix) {
        Matcher matcher = pattern.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        while (matcher.find()) {
            String styleClass;
            if (matcher.group("COMMENT") != null) {
                styleClass = prefix + "-comment";
            } else if (matcher.group("TAG") != null || matcher.group("CLOSETAG") != null) {
                styleClass = prefix + "-tag";
            } else if (matcher.group("ATTR") != null) {
                styleClass = prefix + "-attribute";
            } else if (matcher.group("VALUE") != null) {
                styleClass = prefix + "-value";
            } else {
                styleClass = prefix + "-tag";
            }

            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }

        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    private static StyleSpans<Collection<String>> computeJsHighlighting(String text) {
        Matcher matcher = JS_PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        while (matcher.find()) {
            String styleClass;
            if (matcher.group("KEYWORD") != null) {
                styleClass = "js-keyword";
            } else if (matcher.group("STRING") != null) {
                styleClass = "js-string";
            } else if (matcher.group("NUMBER") != null) {
                styleClass = "js-number";
            } else if (matcher.group("COMMENT") != null) {
                styleClass = "js-comment";
            } else {
                styleClass = null;
            }

            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            if (styleClass != null) {
                spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            } else {
                spansBuilder.add(Collections.emptyList(), matcher.end() - matcher.start());
            }
            lastKwEnd = matcher.end();
        }

        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    private static StyleSpans<Collection<String>> computeHexHighlighting(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        int lastEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                spansBuilder.add(Collections.emptyList(), matcher.start() - lastEnd);
            }

            int offsetStart = matcher.start("OFFSET");
            int offsetEnd = matcher.end("OFFSET");
            int bytesStart = matcher.start("BYTES");
            int bytesEnd = matcher.end("BYTES");
            int gapStart = matcher.start("GAP");
            int gapEnd = matcher.end("GAP");
            int asciiStart = matcher.start("ASCII");
            int asciiEnd = matcher.end("ASCII");

            spansBuilder.add(Collections.singleton("hex-offset"), offsetEnd - offsetStart);
            spansBuilder.add(Collections.singleton("hex-byte"), bytesEnd - bytesStart);
            spansBuilder.add(Collections.emptyList(), gapEnd - gapStart);
            spansBuilder.add(Collections.singleton("hex-ascii"), asciiEnd - asciiStart);

            lastEnd = matcher.end();
        }

        if (text.length() > lastEnd) {
            spansBuilder.add(Collections.emptyList(), text.length() - lastEnd);
        }
        return spansBuilder.create();
    }

    public static String formatXml(String xml) {
        if (xml == null || xml.isBlank()) return xml;
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StreamSource source = new StreamSource(new StringReader(xml.trim()));
            StringWriter writer = new StringWriter();
            transformer.transform(source, new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            return xml;
        }
    }

    public static String formatHexDump(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i += 16) {
            sb.append(String.format("%08x: ", i));
            int lineBytes = Math.min(16, bytes.length - i);
            for (int j = 0; j < 16; j++) {
                if (j < lineBytes) {
                    sb.append(String.format("%02X", bytes[i + j]));
                } else {
                    sb.append("  ");
                }
                if (j < 15) {
                    sb.append(' ');
                }
            }
            sb.append("    ");
            for (int j = 0; j < lineBytes; j++) {
                byte b = bytes[i + j];
                sb.append((b >= 32 && b < 127) ? (char) b : '.');
            }
            if (i + 16 < bytes.length) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    public static String formatBase64(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";
        return Base64.getEncoder().encodeToString(bytes);
    }
}
