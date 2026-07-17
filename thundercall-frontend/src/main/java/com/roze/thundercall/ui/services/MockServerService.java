package com.roze.thundercall.ui.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.roze.thundercall.ui.models.*;
import com.roze.thundercall.ui.utils.AlertUtils;
import javafx.application.Platform;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class MockServerService {
    private static final String BASE_URL = "/mock-servers";

    public static Optional<MockServerResponse> createMockServer(String name, String description) {
        try {
            MockServerRequest request = new MockServerRequest(name, description);
            BaseResponse<MockServerResponse> response = ApiClient.post(BASE_URL, request,
                    new TypeReference<BaseResponse<MockServerResponse>>() {
                    });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to create mock server: " + friendlyMessage(e)));
        }
        return Optional.empty();
    }

    public static Optional<List<MockServerResponse>> getMyMockServers() {
        try {
            BaseResponse<List<MockServerResponse>> response = ApiClient.get(BASE_URL,
                    new TypeReference<BaseResponse<List<MockServerResponse>>>() {
                    });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to load mock servers: " + friendlyMessage(e)));
        }
        return Optional.empty();
    }

    public static Optional<MockServerResponse> getMockServer(Long id) {
        try {
            BaseResponse<MockServerResponse> response = ApiClient.get(BASE_URL + "/" + id,
                    new TypeReference<BaseResponse<MockServerResponse>>() {
                    });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to load mock server: " + friendlyMessage(e)));
        }
        return Optional.empty();
    }

    public static boolean setEnabled(Long id, boolean enabled) {
        try {
            ApiClient.put(BASE_URL + "/" + id + "/enabled?enabled=" + enabled, null,
                    new TypeReference<BaseResponse<Void>>() {
                    });
            return true;
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to update mock server: " + friendlyMessage(e)));
            return false;
        }
    }

    public static boolean deleteMockServer(Long id) {
        try {
            BaseResponse<Void> response = ApiClient.delete(BASE_URL + "/" + id,
                    new TypeReference<BaseResponse<Void>>() {
                    });
            return response != null && response.isSuccess();
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to delete mock server: " + friendlyMessage(e)));
            return false;
        }
    }

    /** Throws so the caller can show the specific reason (e.g. a
     * duplicate method+path) rather than a generic failure message. */
    public static MockRouteResponse addRoute(Long mockServerId, MockRouteRequest request) throws IOException {
        BaseResponse<MockRouteResponse> response = ApiClient.post(BASE_URL + "/" + mockServerId + "/routes", request,
                new TypeReference<BaseResponse<MockRouteResponse>>() {
                });
        if (response == null || !response.isSuccess()) {
            throw new IOException("Invalid response from server");
        }
        return response.getData();
    }

    public static Optional<List<MockRouteResponse>> getRoutes(Long mockServerId) {
        try {
            BaseResponse<List<MockRouteResponse>> response = ApiClient.get(BASE_URL + "/" + mockServerId + "/routes",
                    new TypeReference<BaseResponse<List<MockRouteResponse>>>() {
                    });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to load routes: " + friendlyMessage(e)));
        }
        return Optional.empty();
    }

    public static MockRouteResponse updateRoute(Long mockServerId, Long routeId, MockRouteRequest request) throws IOException {
        BaseResponse<MockRouteResponse> response = ApiClient.put(
                BASE_URL + "/" + mockServerId + "/routes/" + routeId, request,
                new TypeReference<BaseResponse<MockRouteResponse>>() {
                });
        if (response == null || !response.isSuccess()) {
            throw new IOException("Invalid response from server");
        }
        return response.getData();
    }

    public static boolean deleteRoute(Long mockServerId, Long routeId) {
        try {
            BaseResponse<Void> response = ApiClient.delete(BASE_URL + "/" + mockServerId + "/routes/" + routeId,
                    new TypeReference<BaseResponse<Void>>() {
                    });
            return response != null && response.isSuccess();
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to delete route: " + friendlyMessage(e)));
            return false;
        }
    }

    /** Strips the "HTTP 4xx: {...}" wrapper down to the server's own
     * message where possible. */
    public static String friendlyMessage(IOException e) {
        String raw = e.getMessage();
        if (raw == null) {
            return "Something went wrong.";
        }
        int idx = raw.indexOf("\"message\":\"");
        if (idx >= 0) {
            int start = idx + "\"message\":\"".length();
            int end = raw.indexOf('"', start);
            if (end > start) {
                return raw.substring(start, end);
            }
        }
        return raw;
    }
}