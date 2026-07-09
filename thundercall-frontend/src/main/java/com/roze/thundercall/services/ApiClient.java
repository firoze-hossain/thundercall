package com.roze.thundercall.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.roze.thundercall.models.AuthResponse;
import com.roze.thundercall.models.BaseResponse;
import com.roze.thundercall.models.LoginRequest;
import com.roze.thundercall.models.RegisterRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ApiClient {
    private static final String BASE_URL = "http://localhost:8084/api/v1";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static String token;

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

    public static AuthResponse register(String username, String email, String password) throws IOException {
        RegisterRequest request = new RegisterRequest(username, email, password);
        BaseResponse<AuthResponse> wrapper = post("/auth/register", request, new TypeReference<BaseResponse<AuthResponse>>() {
        });
        if (wrapper != null && wrapper.getData() != null) {
            return wrapper.getData();
        } else {
            throw new IOException("Invalid response from server: " + (wrapper != null ? wrapper.getMessage() : "null response"));
        }
    }

    public static void logout() throws IOException {
        BaseResponse<Void> wrapper = post("/auth/logout", null, new TypeReference<BaseResponse<Void>>() {
        });
        if (wrapper == null || (wrapper != null && wrapper.isSuccess())) {
            token = null;
        } else {
            throw new IOException("Logout failed: " + (wrapper != null ? wrapper.getMessage() : "Unknown error"));
        }
    }

    public static <T> T post(String endpoint, Object data, TypeReference<T> responseType) throws IOException {
        HttpURLConnection connection = createConnection(endpoint, "POST");
        if (data != null) {
            String json = objectMapper.writeValueAsString(data);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
        }
        int responseCode = connection.getResponseCode();
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
            String error = readError(connection);
            throw new IOException("HTTP " + responseCode + ": " + error);
        }


    }

    public static <T> BaseResponse<T> get(String endpoint, TypeReference<BaseResponse<T>> responseType) throws IOException {
        HttpURLConnection connection = createConnection(endpoint, "GET");
        return handleResponse(connection, responseType);
    }

    private static <T> BaseResponse<T> handleResponse(HttpURLConnection connection, TypeReference<BaseResponse<T>> responseType) throws IOException {
        int responseCode = connection.getResponseCode();
        if (responseCode == 200 || responseCode == 201) {
            try (InputStream is = connection.getInputStream()) {
                String responseBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                return objectMapper.readValue(responseBody, responseType);

            }
        } else {
            String error = readError(connection);
            throw new IOException("HTTP " + responseCode + ": " + error);
        }
    }

    public static <T> T put(String endpoint, Object data, TypeReference<T> responseType) throws IOException {
        HttpURLConnection connection = createConnection(endpoint, "PUT");
        if (data != null) {
            String json = objectMapper.writeValueAsString(data);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
        }
        int responseCode = connection.getResponseCode();
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
            String error = readError(connection);
            throw new IOException("HTTP " + responseCode + ": " + error);
        }
    }

    public static <T> T patch(String endpoint, Object data, TypeReference<T> responseType) throws IOException {
        HttpURLConnection connection = createConnection(endpoint, "PATCH");
        if (data != null) {
            String json = objectMapper.writeValueAsString(data);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
        }
        int responseCode = connection.getResponseCode();
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
            String error = readError(connection);
            throw new IOException("HTTP " + responseCode + ": " + error);
        }
    }

    public static <T> BaseResponse<T> delete(String endpoint, TypeReference<BaseResponse<T>> responseType) throws IOException {
        HttpURLConnection connection = createConnection(endpoint, "DELETE");
        int responseCode = connection.getResponseCode();
        if (responseCode == 200 || responseCode == 204) {
            if (responseCode == 204) {
                return null;
            }
            try (InputStream is = connection.getInputStream()) {
                String responseBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                return objectMapper.readValue(responseBody, responseType);
            }
        } else {
            String error = readError(connection);
            throw new IOException("HTTP " + responseCode + ": " + error);
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

    private static String readError(HttpURLConnection connection) throws IOException {
        try (InputStream es = connection.getErrorStream()) {
            if (es != null) {
                return new String(es.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        return "Unknown error";
    }

    public static String getToken() {
        return token;
    }

    public static void setToken(String token) {
        ApiClient.token = token;
    }
}
