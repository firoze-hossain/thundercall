package com.roze.thundercall.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.roze.thundercall.models.ApiRequest;
import com.roze.thundercall.models.ApiResponse;
import com.roze.thundercall.models.BaseResponse;
import com.roze.thundercall.models.RequestResponse;
import com.roze.thundercall.utils.AlertUtils;
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
