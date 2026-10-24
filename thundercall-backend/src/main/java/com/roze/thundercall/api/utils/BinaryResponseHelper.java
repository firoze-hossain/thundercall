package com.roze.thundercall.api.utils;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** How to tell a binary response (PDF, Excel, image, zip...) from a
 * text one, and what to call the resulting file. Originally lived
 * only inside RequestServiceImpl — extracted here so the
 * shared-workspace quick-send path (WorkspaceSharingServiceImpl) uses
 * the exact same rules instead of a second copy that quietly handled
 * binary responses differently (or not at all, which is what was
 * actually happening before this existed). */
public final class BinaryResponseHelper {
    private static final Pattern FILENAME_PATTERN =
            Pattern.compile("filename\\*?=\"?([^\";]+)\"?", Pattern.CASE_INSENSITIVE);

    private BinaryResponseHelper() {
    }

    /** Anything that isn't recognizably text is treated as a binary download. */
    public static boolean isBinary(String contentType) {
        return isBinary(contentType, null);
    }

    public static boolean isBinary(String contentType, byte[] body) {
        // 1. Inspect magic bytes if body payload is available
        if (body != null && body.length >= 4) {
            // ZIP / OpenXML (XLSX, DOCX, PPTX, ZIP, JAR, etc.): PK\x03\x04
            if (body[0] == 0x50 && body[1] == 0x4B && body[2] == 0x03 && body[3] == 0x04) {
                return true;
            }
            // PDF: %PDF
            if (body[0] == 0x25 && body[1] == 0x50 && body[2] == 0x44 && body[3] == 0x46) {
                return true;
            }
            // OLE2 / Legacy MS Office (.xls, .doc, .ppt): D0 CF 11 E0
            if (body[0] == (byte) 0xD0 && body[1] == (byte) 0xCF && body[2] == (byte) 0x11 && body[3] == (byte) 0xE0) {
                return true;
            }
            // PNG: \x89PNG
            if (body[0] == (byte) 0x89 && body[1] == 0x50 && body[2] == 0x4E && body[3] == 0x47) {
                return true;
            }
            // JPEG: FF D8 FF
            if ((body[0] & 0xFF) == 0xFF && (body[1] & 0xFF) == 0xD8 && (body[2] & 0xFF) == 0xFF) {
                return true;
            }
            // GIF: GIF8
            if (body[0] == 'G' && body[1] == 'I' && body[2] == 'F' && body[3] == '8') {
                return true;
            }
        }

        if (contentType == null || contentType.isBlank()) {
            return false; // most simple REST APIs omit it; assume text (JSON/plain)
        }
        String ct = contentType.toLowerCase(Locale.ROOT);

        // 2. Binary / Document types: Checked FIRST because Office OpenXML MIME types contain
        // "openxmlformats", which contains "xml" as a substring.
        if (ct.contains("spreadsheet") || ct.contains("excel") || ct.contains("officedocument")
                || ct.contains("pdf") || ct.contains("msword") || ct.contains("wordprocessingml")
                || ct.contains("powerpoint") || ct.contains("presentationml")
                || ct.contains("octet-stream") || ct.contains("zip") || ct.contains("tar")
                || ct.contains("gzip") || ct.contains("7z") || ct.contains("audio") || ct.contains("video")
                || (ct.contains("image") && !ct.contains("svg"))) {
            return true;
        }

        // 3. Known text types (using /xml or +xml so openxmlformats is not matched)
        if (ct.contains("json") || ct.contains("text") || ct.contains("javascript")
                || ct.contains("html") || ct.contains("csv") || ct.contains("yaml")
                || ct.contains("x-www-form-urlencoded")
                || ct.contains("/xml") || ct.contains("+xml") || ct.equals("text/xml")) {
            return false;
        }
        return false;
    }

    public static String decodeText(byte[] body, String contentType) {
        Charset charset = StandardCharsets.UTF_8;
        if (contentType != null) {
            try {
                MediaType mt = MediaType.parseMediaType(contentType);
                if (mt.getCharset() != null) {
                    charset = mt.getCharset();
                }
            } catch (Exception ignored) {
            }
        }
        return new String(body, charset);
    }

    public static String guessFileName(HttpHeaders headers, String url, String contentType) {
        String disposition = headers != null ? headers.getFirst(HttpHeaders.CONTENT_DISPOSITION) : null;
        if (disposition != null) {
            Matcher m = FILENAME_PATTERN.matcher(disposition);
            if (m.find()) {
                String name = m.group(1).replace("UTF-8''", "").trim();
                if (name.startsWith("\"") && name.endsWith("\"") && name.length() > 1) {
                    name = name.substring(1, name.length() - 1);
                }
                return name;
            }
        }
        String lastSegment = url;
        if (lastSegment != null) {
            int q = lastSegment.indexOf('?');
            if (q >= 0) {
                lastSegment = lastSegment.substring(0, q);
            }
            int slash = lastSegment.lastIndexOf('/');
            if (slash >= 0 && slash < lastSegment.length() - 1) {
                String candidate = lastSegment.substring(slash + 1);
                if (candidate.contains(".")) {
                    return candidate;
                }
                if (!candidate.isBlank()) {
                    return candidate + guessExtension(contentType);
                }
            }
        }
        return "response" + guessExtension(contentType);
    }

    public static String guessExtension(String contentType) {
        if (contentType == null) {
            return ".bin";
        }
        String ct = contentType.toLowerCase(Locale.ROOT);
        if (ct.contains("pdf")) return ".pdf";
        if (ct.contains("spreadsheet") || ct.contains("openxmlformats-officedocument.spreadsheetml")) return ".xlsx";
        if (ct.contains("excel")) return ct.contains("sheet") ? ".xlsx" : ".xls";
        if (ct.contains("wordprocessingml")) return ".docx";
        if (ct.contains("msword")) return ".doc";
        if (ct.contains("presentationml") || ct.contains("powerpoint")) return ".pptx";
        if (ct.contains("zip")) return ".zip";
        if (ct.contains("png")) return ".png";
        if (ct.contains("jpeg") || ct.contains("jpg")) return ".jpg";
        if (ct.contains("gif")) return ".gif";
        if (ct.contains("svg")) return ".svg";
        if (ct.contains("csv")) return ".csv";
        if (ct.contains("json")) return ".json";
        if (ct.contains("/xml") || ct.contains("+xml")) return ".xml";
        return ".bin";
    }
}