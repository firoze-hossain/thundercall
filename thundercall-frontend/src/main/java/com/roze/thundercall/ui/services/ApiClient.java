package com.roze.thundercall.ui.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.roze.thundercall.ui.models.AuthResponse;
import com.roze.thundercall.ui.models.BaseResponse;
import com.roze.thundercall.ui.models.LoginRequest;
import com.roze.thundercall.ui.models.RegisterRequest;
import com.roze.thundercall.ui.models.RegisterResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * ROOT-CAUSE FIX: your access token expires after 1 hour
 * (application.yml jwt.expiration = 3600000ms), and nothing ever refreshed
 * it. Any call made after that point hit Spring Security's default
 * rejection for an unauthenticated request — which returns 403 with an
 * EMPTY body (no BaseResponse envelope), hence the unhelpful
 * "HTTP 403: Unknown error" you saw mid-import.
 * <p>
 * Every request now retries ONCE, automatically, after refreshing the
 * token, whenever it gets a 401/403. Only if the refresh token itself has
 * also expired does the user see anything — and now it's a clear
 * "Your session has expired, please log in again" instead of a cryptic
 * "Unknown error", plus the app returns them to the login screen.
 */
public class ApiClient {
    private static final String BASE_URL = "http://localhost:9084/api/v1";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static String token;
    private static String refreshToken;
    /**
     * Registered once at startup so this low-level class can trigger a
     * graceful return to the login screen without depending on the UI.
     */
    private static Runnable onSessionExpired;

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public static AuthResponse login(String usernameOrEmail, String password) throws IOException {
        LoginRequest loginRequest = new LoginRequest(usernameOrEmail, password);
        BaseResponse<AuthResponse> wrapper = post("/auth/login", loginRequest, new TypeReference<BaseResponse<AuthResponse>>() {
        });
        if (wrapper != null && wrapper.getData() != null) {
            return wrapper.getData();
        } else {
            throw new IOException("Invalid response from server: " + (wrapper != null ? wrapper.getMessage() : "null response"));
        }
    }

    public static RegisterResponse register(String username, String email, String password) throws IOException {
        RegisterRequest request = new RegisterRequest(username, email, password);
        BaseResponse<RegisterResponse> wrapper = post("/auth/register", request, new TypeReference<BaseResponse<RegisterResponse>>() {
        });
        if (wrapper != null && wrapper.getData() != null) {
            return wrapper.getData();
        } else {
            throw new IOException("Invalid response from server: " + (wrapper != null ? wrapper.getMessage() : "null response"));
        }
    }

    public static AuthResponse verifyEmail(String email, String code) throws IOException {
        java.util.Map<String, String> request = new java.util.HashMap<>();
        request.put("email", email);
        request.put("code", code);
        BaseResponse<AuthResponse> wrapper = post("/auth/verify-email", request, new TypeReference<BaseResponse<AuthResponse>>() {
        });
        if (wrapper != null && wrapper.getData() != null) {
            return wrapper.getData();
        } else {
            throw new IOException("Invalid response from server: " + (wrapper != null ? wrapper.getMessage() : "null response"));
        }
    }

    public static void resendVerificationCode(String email) throws IOException {
        java.util.Map<String, String> request = new java.util.HashMap<>();
        request.put("email", email);
        post("/auth/resend-verification", request, new TypeReference<BaseResponse<Void>>() {
        });
    }

    public static <T> T post(String endpoint, Object data, TypeReference<T> responseType) throws IOException {
        return post(endpoint, data, responseType, 2);
    }

    private static <T> T post(String endpoint, Object data, TypeReference<T> responseType, int retriesLeft) throws IOException {
        HttpURLConnection connection = createConnection(endpoint, "POST");
        writeJsonBody(connection, data);
        int responseCode = connection.getResponseCode();

        if (isAuthError(responseCode) && retriesLeft > 0 && attemptTokenRefresh()) {
            return post(endpoint, data, responseType, retriesLeft - 1);
        }
        if (responseCode == 200 || responseCode == 201 || responseCode == 204) {
            if (responseCode == 204) {
                return null;
            }
            try (InputStream is = connection.getInputStream()) {
                String responseBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                System.out.println("Raw Response: " + responseBody);
                return objectMapper.readValue(responseBody, responseType);
            }
        } else {
            throw new IOException(describeError("POST", endpoint, responseCode, connection));
        }
    }

    public static <T> BaseResponse<T> get(String endpoint, TypeReference<BaseResponse<T>> responseType) throws IOException {
        return get(endpoint, responseType, 2);
    }

    private static <T> BaseResponse<T> get(String endpoint, TypeReference<BaseResponse<T>> responseType, int retriesLeft) throws IOException {
        HttpURLConnection connection = createConnection(endpoint, "GET");
        int responseCode = connection.getResponseCode();

        if (isAuthError(responseCode) && retriesLeft > 0 && attemptTokenRefresh()) {
            return get(endpoint, responseType, retriesLeft - 1);
        }
        if (responseCode == 200 || responseCode == 201) {
            try (InputStream is = connection.getInputStream()) {
                String responseBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                return objectMapper.readValue(responseBody, responseType);
            }
        } else {
            throw new IOException(describeError("GET", endpoint, responseCode, connection));
        }
    }

    public static <T> T put(String endpoint, Object data, TypeReference<T> responseType) throws IOException {
        return put(endpoint, data, responseType, 2);
    }

    private static <T> T put(String endpoint, Object data, TypeReference<T> responseType, int retriesLeft) throws IOException {
        HttpURLConnection connection = createConnection(endpoint, "PUT");
        writeJsonBody(connection, data);
        int responseCode = connection.getResponseCode();

        if (isAuthError(responseCode) && retriesLeft > 0 && attemptTokenRefresh()) {
            return put(endpoint, data, responseType, retriesLeft - 1);
        }
        if (responseCode == 200 || responseCode == 201 || responseCode == 204) {
            if (responseCode == 204) {
                return null;
            }
            try (InputStream is = connection.getInputStream()) {
                String responseBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                System.out.println("Raw Response: " + responseBody);
                return objectMapper.readValue(responseBody, responseType);
            }
        } else {
            throw new IOException(describeError("PUT", endpoint, responseCode, connection));
        }
    }

    public static <T> T patch(String endpoint, Object data, TypeReference<T> responseType) throws IOException {
        return patch(endpoint, data, responseType, 2);
    }

    private static <T> T patch(String endpoint, Object data, TypeReference<T> responseType, int retriesLeft) throws IOException {
        HttpURLConnection connection = createConnection(endpoint, "PATCH");
        writeJsonBody(connection, data);
        int responseCode = connection.getResponseCode();

        if (isAuthError(responseCode) && retriesLeft > 0 && attemptTokenRefresh()) {
            return patch(endpoint, data, responseType, retriesLeft - 1);
        }
        if (responseCode == 200 || responseCode == 201 || responseCode == 204) {
            if (responseCode == 204) {
                return null;
            }
            try (InputStream is = connection.getInputStream()) {
                String responseBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                System.out.println("Raw Response: " + responseBody);
                return objectMapper.readValue(responseBody, responseType);
            }
        } else {
            throw new IOException(describeError("PATCH", endpoint, responseCode, connection));
        }
    }

    public static <T> BaseResponse<T> delete(String endpoint, TypeReference<BaseResponse<T>> responseType) throws IOException {
        return delete(endpoint, responseType, 2);
    }

    private static <T> BaseResponse<T> delete(String endpoint, TypeReference<BaseResponse<T>> responseType, int retriesLeft) throws IOException {
        HttpURLConnection connection = createConnection(endpoint, "DELETE");
        int responseCode = connection.getResponseCode();

        if (isAuthError(responseCode) && retriesLeft > 0 && attemptTokenRefresh()) {
            return delete(endpoint, responseType, retriesLeft - 1);
        }
        if (responseCode == 200 || responseCode == 204) {
            if (responseCode == 204) {
                // 204 IS success — the server deleted successfully and simply
                // returned no body. Treating this as null/failure was a bug.
                BaseResponse<T> ok = new BaseResponse<>();
                ok.setSuccess(true);
                ok.setMessage("No content");
                return ok;
            }
            try (InputStream is = connection.getInputStream()) {
                String responseBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                return objectMapper.readValue(responseBody, responseType);
            }
        } else {
            throw new IOException(describeError("DELETE", endpoint, responseCode, connection));
        }
    }

    // ------------------------------------------------------------------

    private static boolean isAuthError(int responseCode) {
        return responseCode == 401 || responseCode == 403;
    }

    /**
     * Calls the backend's existing /auth/refresh endpoint (already
     * implemented server-side, just never used by the client) to get a new
     * access token without forcing a re-login. Returns false if there's no
     * refresh token, or it's also expired — at which point the person is
     * returned to the login screen with a clear explanation instead of a
     * confusing per-request error.
     */
    private static synchronized boolean attemptTokenRefresh() {
        if (refreshToken == null || refreshToken.isEmpty()) {
            System.out.println("[ApiClient] Refresh skipped: no refresh token in memory "
                    + "(likely already logged out, or a session that started before this build)");
            handleSessionExpired();
            return false;
        }
        try {
            String encoded = URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);
            URL url = new URL(BASE_URL + "/auth/refresh?refreshToken=" + encoded);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(false);

            int code = connection.getResponseCode();
            if (code == 200) {
                try (InputStream is = connection.getInputStream()) {
                    String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    BaseResponse<AuthResponse> wrapper = objectMapper.readValue(
                            body, new TypeReference<BaseResponse<AuthResponse>>() {
                            });
                    if (wrapper != null && wrapper.getData() != null && wrapper.getData().getToken() != null) {
                        // Persists the new access + refresh token pair and
                        // updates this class's in-memory token too.
                        TokenManager.storeTokens(wrapper.getData());
                        System.out.println("[ApiClient] Token refreshed successfully");
                        return true;
                    }
                    System.out.println("[ApiClient] Refresh returned 200 but no token in the response: " + body);
                }
            } else {
                System.out.println("[ApiClient] Refresh call returned HTTP " + code + ": " + readError(connection));
            }
        } catch (Exception e) {
            System.out.println("[ApiClient] Refresh call threw an exception: " + e);
        }
        handleSessionExpired();
        return false;
    }

    private static void handleSessionExpired() {
        token = null;
        refreshToken = null;
        TokenManager.clearTokens();
        if (onSessionExpired != null) {
            javafx.application.Platform.runLater(onSessionExpired);
        }
    }

    /**
     * Registered once at startup (see Main) so an expired session returns
     * the person to the login screen instead of failing silently mid-task.
     */
    public static void setOnSessionExpired(Runnable callback) {
        onSessionExpired = callback;
    }

    private static void writeJsonBody(HttpURLConnection connection, Object data) throws IOException {
        if (data != null) {
            String json = objectMapper.writeValueAsString(data);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private static HttpURLConnection createConnection(String endpoint, String method) throws IOException {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        if (token != null && !token.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + token);
        }
        return connection;
    }

    /**
     * Builds a clear error message even when the server returns an empty
     * body (as Spring Security's default rejection does for 401/403).
     */
    /** Prints the TRUE raw failure (before any friendly rewriting) and
     * returns the user-facing message. Kept separate so a future log
     * always shows exactly what the server actually sent back, not just
     * the simplified "session expired" text — that distinction is the
     * whole reason this method exists after debugging a repeat failure
     * that survived two successful token refreshes in a row. */
    private static String describeError(String method, String endpoint, int responseCode,
                                        HttpURLConnection connection) throws IOException {
        String body = readError(connection);
        System.out.println("[ApiClient] " + method + " " + endpoint + " failed: HTTP " + responseCode
                + ", raw body: " + (body == null || body.isBlank() ? "(empty)" : body));
        if (isAuthError(responseCode) && (body == null || body.isBlank() || body.equals("Unknown error"))) {
            return "HTTP " + responseCode + ": Your session has expired — please log in again";
        }
        return "HTTP " + responseCode + ": " + body;
    }

    private static String readError(HttpURLConnection connection) throws IOException {
        try (InputStream es = connection.getErrorStream()) {
            if (es != null) {
                String body = new String(es.readAllBytes(), StandardCharsets.UTF_8);
                return body.isBlank() ? "Unknown error" : body;
            }
        }
        return "Unknown error";
    }

    public static void logout() {
        if (refreshToken != null && !refreshToken.isEmpty()) {
            try {
                String encoded = URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);
                HttpURLConnection connection = createConnection("/auth/logout?refreshToken=" + encoded, "POST");
                connection.setDoOutput(false);
                int code = connection.getResponseCode();
                if (code != 200 && code != 204) {
                    System.out.println("Logout endpoint returned " + code + ": " + readError(connection));
                }
            } catch (Exception e) {
                System.out.println("Logout call failed (logging out locally anyway): " + e.getMessage());
            }
        }
        token = null;
        refreshToken = null;
    }

    public static String getToken() {
        return token;
    }

    public static void setToken(String token) {
        ApiClient.token = token;
    }

    public static void setRefreshToken(String refreshToken) {
        ApiClient.refreshToken = refreshToken;
    }
}