package com.roze.thundercall.ui.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.roze.thundercall.ui.models.ApiRequest;
import com.roze.thundercall.ui.models.ApiResponse;
import com.roze.thundercall.ui.models.BaseResponse;
import com.roze.thundercall.ui.models.RequestResponse;
import com.roze.thundercall.ui.utils.AlertUtils;
import javafx.application.Platform;

import java.io.IOException;
import java.util.Optional;

public class RequestService {
    private static final String BASE_URL = "/requests";

    public static Optional<ApiResponse> executeRequest(ApiRequest request) {
        try {
            BaseResponse<ApiResponse> response = ApiClient.post(BASE_URL + "/execute", request, new TypeReference<BaseResponse<ApiResponse>>() {
            });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to execute request: " + e.getMessage()));
        }
        return Optional.empty();
    }


    /** Deletes a saved request on the server. */
    public static boolean deleteRequest(Long id) {
        try {
            BaseResponse<Void> response = ApiClient.delete(BASE_URL + "/" + id,
                    new TypeReference<BaseResponse<Void>>() {
                    });
            return response != null && response.isSuccess();
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to delete request: " + e.getMessage()));
            return false;
        }
    }

    /** Saves the current editor state back into an EXISTING request (Ctrl+S). */
    public static Optional<RequestResponse> updateRequest(Long id, ApiRequest apiRequest) {
        try {
            BaseResponse<RequestResponse> response = ApiClient.put(BASE_URL + "/" + id, apiRequest,
                    new TypeReference<BaseResponse<RequestResponse>>() {
                    });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to save request: " + e.getMessage()));
        }
        return Optional.empty();
    }

    public static Optional<RequestResponse> saveRequest(ApiRequest request) {
        try {
            BaseResponse<RequestResponse> response = ApiClient.post(BASE_URL, request, new TypeReference<BaseResponse<RequestResponse>>() {
            });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to save request: " + e.getMessage()));
        }
        return Optional.empty();
    }

    public static Optional<RequestResponse> getRequest(Long id) {
        try {
            BaseResponse<RequestResponse> response = ApiClient.get(BASE_URL + "/" + id, new TypeReference<BaseResponse<RequestResponse>>() {
            });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to get request: " + e.getMessage()));
        }
        return Optional.empty();
    }
}