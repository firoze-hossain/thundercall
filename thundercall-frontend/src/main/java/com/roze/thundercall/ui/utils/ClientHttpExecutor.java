package com.roze.thundercall.ui.utils;

import com.roze.thundercall.ui.models.ApiResponse;
import com.roze.thundercall.ui.models.FormDataField;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Executes an HTTP request directly from this machine — the desktop
 * app — instead of proxying it through the backend server the way
 * RequestServiceImpl/WorkspaceSharingServiceImpl do. This matters as
 * soon as the backend is hosted somewhere other than your own PC: a
 * server-executed request to "localhost" or an address only your own
 * network can reach resolves on the SERVER, not on your machine —
 * which silently breaks testing anything local or internal. Real
 * Postman's desktop app executes this same way for exactly this
 * reason (its cloud/web client is the one that needs a local "Agent"
 * helper to get around it — the desktop app never had the problem in
 * the first place, since it always executed client-side). */
public class ClientHttpExecutor {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final Pattern FILENAME_PATTERN =
            Pattern.compile("filename\\*?=\"?([^\";]+)\"?", Pattern.CASE_INSENSITIVE);

    public static ApiResponse execute(String method, String url, String headersJson, String body) {
        Instant start = Instant.now();
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(60));

            boolean hasContentType = applyHeaders(builder, headersJson);

            String upperMethod = method == null ? "GET" : method.toUpperCase(Locale.ROOT);
            boolean hasBody = body != null && !body.isBlank();
            // Same default the backend already applies — most people
            // don't bother setting Content-Type by hand for a JSON body.
            if (!hasContentType && hasBody) {
                builder.header("Content-Type", "application/json");
            }

            HttpRequest.BodyPublisher publisher = hasBody
                    ? HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)
                    : HttpRequest.BodyPublishers.noBody();
            applyMethod(builder, upperMethod, hasBody, publisher);

            return send(builder, url, start);
        } catch (Exception e) {
            return errorResponse(e, start);
        }
    }

    /** Builds a genuine multipart/form-data body — text fields as plain
     * parts, file fields as their real bytes (decoded from the Base64
     * already read off disk on the FX thread before this was called) —
     * matching what RequestServiceImpl.buildMultipartBody() does on the
     * backend, just assembled by hand since java.net.http has no
     * built-in multipart support the way Spring's RestTemplate does. */
    public static ApiResponse executeMultipart(String method, String url, String headersJson, List<FormDataField> formData) {
        Instant start = Instant.now();
        try {
            String boundary = "ThundercallBoundary" + System.currentTimeMillis();
            byte[] bodyBytes = buildMultipartBody(formData, boundary);

            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary);

            // User headers apply too, EXCEPT Content-Type — that one has to
            // stay exactly what was just set above (it carries the boundary).
            applyHeaders(builder, headersJson, "content-type");

            String upperMethod = method == null ? "POST" : method.toUpperCase(Locale.ROOT);
            builder.method(upperMethod, HttpRequest.BodyPublishers.ofByteArray(bodyBytes));

            return send(builder, url, start);
        } catch (Exception e) {
            return errorResponse(e, start);
        }
    }

    private static byte[] buildMultipartBody(List<FormDataField> formData, String boundary) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (formData != null) {
            for (FormDataField field : formData) {
                if (field.getKey() == null || field.getKey().isBlank()) {
                    continue;
                }
                out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                if ("file".equalsIgnoreCase(field.getType())) {
                    if (field.getFileBase64() == null || field.getFileBase64().isBlank()) {
                        continue; // matches the backend: a file row with nothing chosen is skipped
                    }
                    String fileName = field.getFileName() != null ? field.getFileName() : "file";
                    out.write(("Content-Disposition: form-data; name=\"" + field.getKey()
                            + "\"; filename=\"" + fileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
                    out.write("Content-Type: application/octet-stream\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                    out.write(Base64.getDecoder().decode(field.getFileBase64()));
                    out.write("\r\n".getBytes(StandardCharsets.UTF_8));
                } else {
                    out.write(("Content-Disposition: form-data; name=\"" + field.getKey() + "\"\r\n\r\n")
                            .getBytes(StandardCharsets.UTF_8));
                    out.write((field.getValue() != null ? field.getValue() : "").getBytes(StandardCharsets.UTF_8));
                    out.write("\r\n".getBytes(StandardCharsets.UTF_8));
                }
            }
        }
        out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    /** Applies every header in headersJson except any name listed in
     * skipKeys (case-insensitive). Returns whether a Content-Type was
     * present among the applied headers. */
    private static boolean applyHeaders(HttpRequest.Builder builder, String headersJson, String... skipKeys) {
        boolean hasContentType = false;
        if (headersJson == null || headersJson.isBlank()) {
            return false;
        }
        try {
            JSONObject obj = new JSONObject(headersJson);
            Iterator<String> keys = obj.keys();
            outer:
            while (keys.hasNext()) {
                String key = keys.next();
                if (key == null || key.isBlank()) {
                    continue;
                }
                for (String skip : skipKeys) {
                    if (skip.equalsIgnoreCase(key.trim())) {
                        continue outer;
                    }
                }
                Object rawValue = obj.get(key);
                String value = rawValue == null ? null : String.valueOf(rawValue);
                if (value == null) {
                    continue;
                }
                builder.header(key.trim(), value);
                if ("content-type".equalsIgnoreCase(key.trim())) {
                    hasContentType = true;
                }
            }
        } catch (Exception ignored) {
            // malformed headers JSON — proceed without them rather than failing outright
        }
        return hasContentType;
    }

    private static void applyMethod(HttpRequest.Builder builder, String upperMethod, boolean hasBody, HttpRequest.BodyPublisher publisher) {
        switch (upperMethod) {
            case "GET":
                builder.GET();
                break;
            case "HEAD":
                builder.method("HEAD", HttpRequest.BodyPublishers.noBody());
                break;
            case "DELETE":
                builder.method("DELETE", hasBody ? publisher : HttpRequest.BodyPublishers.noBody());
                break;
            default:
                builder.method(upperMethod, publisher);
                break;
        }
    }

    private static ApiResponse send(HttpRequest.Builder builder, String url, Instant start) throws Exception {
        HttpResponse<byte[]> response = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        long duration = Duration.between(start, Instant.now()).toMillis();

        byte[] responseBytes = response.body() != null ? response.body() : new byte[0];
        String contentType = response.headers().firstValue("Content-Type").orElse(null);
        boolean binary = isBinary(contentType);

        StringBuilder headersOut = new StringBuilder();
        response.headers().map().forEach((k, v) ->
                headersOut.append(k).append(": ").append(String.join(", ", v)).append("\n"));

        return ApiResponse.builder()
                .statusCode(response.statusCode())
                .response(binary
                        ? Base64.getEncoder().encodeToString(responseBytes)
                        : new String(responseBytes, StandardCharsets.UTF_8))
                .responseHeaders(headersOut.toString())
                .duration(duration)
                .success(response.statusCode() < 400)
                .binary(binary)
                .contentType(contentType)
                .fileName(binary ? guessFileName(response, url, contentType) : null)
                .sizeBytes(responseBytes.length)
                .build();
    }

    private static ApiResponse errorResponse(Exception e, Instant start) {
        long duration = Duration.between(start, Instant.now()).toMillis();
        String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        return ApiResponse.builder()
                .statusCode(0)
                .response("Could not connect: " + message)
                .duration(duration)
                .success(false)
                .binary(false)
                .sizeBytes(0)
                .build();
    }

    /** Same rules as the backend's BinaryResponseHelper.isBinary() —
     * kept consistent on purpose so a response looks and behaves the
     * same whether it happened to be sent client-side or server-side. */
    private static boolean isBinary(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return false;
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

    private static String guessFileName(HttpResponse<byte[]> response, String url, String contentType) {
        String disposition = response.headers().firstValue("Content-Disposition").orElse(null);
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

    private static String guessExtension(String contentType) {
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