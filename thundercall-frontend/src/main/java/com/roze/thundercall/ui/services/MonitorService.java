package com.roze.thundercall.ui.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.roze.thundercall.ui.models.BaseResponse;
import com.roze.thundercall.ui.models.MonitorRequest;
import com.roze.thundercall.ui.models.MonitorResponse;
import com.roze.thundercall.ui.models.MonitorRunResponse;
import com.roze.thundercall.ui.utils.AlertUtils;
import javafx.application.Platform;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class MonitorService {
    private static final String BASE_URL = "/monitors";

    /** Throws so the caller can show the specific reason (e.g. an
     * invalid interval or a missing collection) rather than a generic
     * failure message. */
    public static MonitorResponse createMonitor(MonitorRequest request) throws IOException {
        BaseResponse<MonitorResponse> response = ApiClient.post(BASE_URL, request,
                new TypeReference<BaseResponse<MonitorResponse>>() {
                });
        if (response == null || !response.isSuccess()) {
            throw new IOException("Invalid response from server");
        }
        return response.getData();
    }

    public static Optional<List<MonitorResponse>> getMyMonitors() {
        try {
            BaseResponse<List<MonitorResponse>> response = ApiClient.get(BASE_URL,
                    new TypeReference<BaseResponse<List<MonitorResponse>>>() {
                    });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to load monitors: " + friendlyMessage(e)));
        }
        return Optional.empty();
    }

    public static MonitorResponse updateMonitor(Long id, MonitorRequest request) throws IOException {
        BaseResponse<MonitorResponse> response = ApiClient.put(BASE_URL + "/" + id, request,
                new TypeReference<BaseResponse<MonitorResponse>>() {
                });
        if (response == null || !response.isSuccess()) {
            throw new IOException("Invalid response from server");
        }
        return response.getData();
    }

    public static boolean deleteMonitor(Long id) {
        try {
            BaseResponse<Void> response = ApiClient.delete(BASE_URL + "/" + id,
                    new TypeReference<BaseResponse<Void>>() {
                    });
            return response != null && response.isSuccess();
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to delete monitor: " + friendlyMessage(e)));
            return false;
        }
    }

    public static Optional<List<MonitorRunResponse>> getRuns(Long id) {
        try {
            BaseResponse<List<MonitorRunResponse>> response = ApiClient.get(BASE_URL + "/" + id + "/runs",
                    new TypeReference<BaseResponse<List<MonitorRunResponse>>>() {
                    });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to load run history: " + friendlyMessage(e)));
        }
        return Optional.empty();
    }

    /** Throws so the caller can show a real error rather than a silent
     * no-op if the run itself couldn't even start. */
    public static MonitorRunResponse runNow(Long id) throws IOException {
        BaseResponse<MonitorRunResponse> response = ApiClient.post(BASE_URL + "/" + id + "/run-now", null,
                new TypeReference<BaseResponse<MonitorRunResponse>>() {
                });
        if (response == null || !response.isSuccess()) {
            throw new IOException("Invalid response from server");
        }
        return response.getData();
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