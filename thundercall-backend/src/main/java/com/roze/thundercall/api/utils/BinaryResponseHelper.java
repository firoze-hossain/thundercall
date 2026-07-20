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
        if (contentType == null || contentType.isBlank()) {
            return false; // most simple REST APIs omit it; assume text (JSON/plain)
        }
        String ct = contentType.toLowerCase(Locale.ROOT);
        if (ct.contains("json") || ct.contains("xml") || ct.contains("text")
                || ct.contains("javascript") || ct.contains("html") || ct.contains("csv")
                || ct.contains("yaml") || ct.contains("x-www-form-urlencoded")) {
            return false;
        }
        return ct.contains("pdf") || ct.contains("excel") || ct.contains("spreadsheet")
                || ct.contains("octet-stream") || ct.contains("zip") || ct.contains("image")
                || ct.contains("audio") || ct.contains("video") || ct.contains("msword")
                || ct.contains("officedocument");
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
        String disposition = headers.getFirst(HttpHeaders.CONTENT_DISPOSITION);
        if (disposition != null) {
            Matcher m = FILENAME_PATTERN.matcher(disposition);
            if (m.find()) {
                return m.group(1).replace("UTF-8''", "");
            }
        }
        String lastSegment = url;
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
        }
        return "response" + guessExtension(contentType);
    }

    public static String guessExtension(String contentType) {
        if (contentType == null) {
            return ".bin";
        }
        String ct = contentType.toLowerCase(Locale.ROOT);
        if (ct.contains("pdf")) return ".pdf";
        if (ct.contains("spreadsheet") || ct.contains("excel")) return ".xlsx";
        if (ct.contains("msword") || ct.contains("wordprocessingml")) return ".docx";
        if (ct.contains("zip")) return ".zip";
        if (ct.contains("png")) return ".png";
        if (ct.contains("jpeg") || ct.contains("jpg")) return ".jpg";
        if (ct.contains("csv")) return ".csv";
        return ".bin";
    }
}