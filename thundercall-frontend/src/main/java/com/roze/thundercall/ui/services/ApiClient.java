package com.roze.thundercall.ui.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.roze.thundercall.ui.models.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Robust HTTP client for ThunderCall API.
 * Handles automatic JWT refresh, connection timeouts, non-blocking logout,
 * and debounced session expiration alerts.
 */
public class ApiClient {
    private static final String DEFAULT_BASE_URL = "http://localhost:9084/api/v1";
    private static final String BASE_URL = resolveBaseUrl();

    /**
     * Resolves the API base URL in order of priority:
     * 1. JVM System Property (-Dthundercall.api.url=...)
     * 2. OS Environment variable (THUNDERCALL_API_URL=...)
     * 3. Embedded config properties file (/app-config.properties injected by Maven during packaging)
     * 4. Fallback default for local development (http://localhost:9084/api/v1)
     */
    private static String resolveBaseUrl() {
        // 1. Check JVM System Property
        String sysProp = System.getProperty("thundercall.api.url");
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp.trim();
        }

        // 2. Check OS Environment variable
        String envVar = System.getenv("THUNDERCALL_API_URL");
        if (envVar != null && !envVar.isBlank()) {
            return envVar.trim();
        }

        // 3. Check build-time filtered config properties file (populated by GitHub Actions / Maven)
        try (InputStream in = ApiClient.class.getResourceAsStream("/app-config.properties")) {
            if (in != null) {
                java.util.Properties props = new java.util.Properties();
                props.load(in);
                String propUrl = props.getProperty("api.base.url");
                if (propUrl != null && !propUrl.isBlank() && !propUrl.startsWith("${")) {
                    return propUrl.trim();
                }
            }
        } catch (Exception ignored) {
        }

        // 4. Default fallback for local development
        return DEFAULT_BASE_URL;
    }
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static String token;
    private static String refreshToken;
    
    /** Single-flight gate for session expiration to prevent cascading alert popups. */
    private static final AtomicBoolean sessionExpiredNotified = new AtomicBoolean(false);
    private static volatile boolean userLoggedOut = false;

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

    public static void markLoggedIn() {
        userLoggedOut = false;
        sessionExpiredNotified.set(false);
    }

    public static void markLoggedOut() {
        userLoggedOut = true;
        sessionExpiredNotified.set(true); // Suppress any in-flight background 401s after manual logout
    }

    public static AuthResponse login(String usernameOrEmail, String password) throws IOException {
        markLoggedIn();
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

    public static AuthResponse ssoLogin(String email, String username, String token, String provider) throws IOException {
        markLoggedIn();
        java.util.Map<String, String> request = new java.util.HashMap<>();
        request.put("email", email);
        request.put("username", username);
        request.put("token", token);
        request.put("provider", provider != null ? provider : "rozehub");
        BaseResponse<AuthResponse> wrapper = post("/auth/sso", request, new TypeReference<BaseResponse<AuthResponse>>() {
        });
        if (wrapper != null && wrapper.getData() != null) {
            return wrapper.getData();
        } else {
            throw new IOException("Invalid response from server: " + (wrapper != null ? wrapper.getMessage() : "null response"));
        }
    }

    public static <T> T post(String endpoint, Object data, TypeReference<T> responseType) throws IOException {
        return post(endpoint, data, responseType, 2);
    }

    private static <T> T post(String endpoint, Object data, TypeReference<T> responseType, int retriesLeft) throws IOException {
        HttpURLConnection connection = createConnection(endpoint, "POST");
        writeJsonBody(connection, data);
        int responseCode = connection.getResponseCode();

        if (isAuthError(endpoint, responseCode) && retriesLeft > 0 && attemptTokenRefresh()) {
            return post(endpoint, data, responseType, retriesLeft - 1);
        }
        if (responseCode == 200 || responseCode == 201 || responseCode == 204) {
            if (responseCode == 204) {
                return null;
            }
            try (InputStream is = connection.getInputStream()) {
                String responseBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
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

        if (isAuthError(endpoint, responseCode) && retriesLeft > 0 && attemptTokenRefresh()) {
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

        if (isAuthError(endpoint, responseCode) && retriesLeft > 0 && attemptTokenRefresh()) {
            return put(endpoint, data, responseType, retriesLeft - 1);
        }
        if (responseCode == 200 || responseCode == 201 || responseCode == 204) {
            if (responseCode == 204) {
                return null;
            }
            try (InputStream is = connection.getInputStream()) {
                String responseBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
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

        if (isAuthError(endpoint, responseCode) && retriesLeft > 0 && attemptTokenRefresh()) {
            return patch(endpoint, data, responseType, retriesLeft - 1);
        }
        if (responseCode == 200 || responseCode == 201 || responseCode == 204) {
            if (responseCode == 204) {
                return null;
            }
            try (InputStream is = connection.getInputStream()) {
                String responseBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
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

        if (isAuthError(endpoint, responseCode) && retriesLeft > 0 && attemptTokenRefresh()) {
            return delete(endpoint, responseType, retriesLeft - 1);
        }
        if (responseCode == 200 || responseCode == 204) {
            if (responseCode == 204) {
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

    private static boolean isAuthError(String endpoint, int responseCode) {
        // Never attempt token refresh on explicit authentication endpoints
        if (endpoint != null && endpoint.startsWith("/auth/")) {
            return false;
        }
        return responseCode == 401 || responseCode == 403;
    }

    private static volatile long lastRefreshTime = 0;

    /**
     * Calls the backend's /auth/refresh endpoint to get a new access token.
     * Returns false if there's no refresh token or session has expired.
     */
    private static synchronized boolean attemptTokenRefresh() {
        if (userLoggedOut) {
            return false;
        }

        if (TokenManager.isSessionExpired()) {
            System.out.println("[ApiClient] 7-day session has expired. Halting refresh and redirecting to login.");
            handleSessionExpired();
            return false;
        }

        long now = System.currentTimeMillis();
        if (now - lastRefreshTime < 4000 && token != null && !token.isEmpty()) {
            return true;
        }

        if (refreshToken == null || refreshToken.isEmpty()) {
            System.out.println("[ApiClient] Refresh skipped: no refresh token in memory.");
            handleSessionExpired();
            return false;
        }
        try {
            String encoded = URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);
            URL url = new URL(BASE_URL + "/auth/refresh?refreshToken=" + encoded);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(4000);
            connection.setReadTimeout(6000);
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
                        TokenManager.storeTokens(wrapper.getData());
                        lastRefreshTime = System.currentTimeMillis();
                        System.out.println("[ApiClient] Token refreshed successfully");
                        return true;
                    }
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
        if (userLoggedOut || (token == null && refreshToken == null)) {
            token = null;
            refreshToken = null;
            return;
        }

        token = null;
        refreshToken = null;
        TokenManager.clearTokens();

        if (sessionExpiredNotified.compareAndSet(false, true)) {
            if (onSessionExpired != null) {
                javafx.application.Platform.runLater(onSessionExpired);
            }
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
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        if (token != null && !token.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + token);
        }
        return connection;
    }

    private static String describeError(String method, String endpoint, int responseCode,
                                        HttpURLConnection connection) throws IOException {
        String body = readError(connection);
        System.out.println("[ApiClient] " + method + " " + endpoint + " failed: HTTP " + responseCode
                + ", raw body: " + (body == null || body.isBlank() ? "(empty)" : body));
        if (isAuthError(endpoint, responseCode) && (body == null || body.isBlank() || body.equals("Unknown error"))) {
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
        markLoggedOut();
        final String ref = refreshToken;
        token = null;
        refreshToken = null;
        if (ref != null && !ref.isEmpty()) {
            // Run server-side token invalidation asynchronously so the UI thread NEVER freezes
            new Thread(() -> {
                try {
                    String encoded = URLEncoder.encode(ref, StandardCharsets.UTF_8);
                    HttpURLConnection connection = createConnection("/auth/logout?refreshToken=" + encoded, "POST");
                    connection.setConnectTimeout(2000);
                    connection.setReadTimeout(2000);
                    connection.setDoOutput(false);
                    connection.getResponseCode();
                } catch (Exception ignored) {
                }
            }, "thundercall-logout-thread").start();
        }
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
