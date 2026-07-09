package com.roze.thundercall.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.roze.thundercall.models.BaseResponse;
import com.roze.thundercall.models.RequestHistoryResponse;
import com.roze.thundercall.utils.AlertUtils;
import javafx.application.Platform;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class RequestHistoryService {
    private static final String BASE_URL = "/history";

    public static Optional<List<RequestHistoryResponse>> getUserRequestHistory() {
        try {
            BaseResponse<List<RequestHistoryResponse>> response = ApiClient.get(BASE_URL,
                    new TypeReference<BaseResponse<List<RequestHistoryResponse>>>() {
                    });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to get request history: " + e.getMessage()));
        }
        return Optional.empty();
    }

    public static Optional<List<RequestHistoryResponse>> getRequestHistory(Long requestId) {
        try {
            BaseResponse<List<RequestHistoryResponse>> response = ApiClient.get(BASE_URL + "/request/" + requestId,
                    new TypeReference<BaseResponse<List<RequestHistoryResponse>>>() {
                    });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to get request history: " + e.getMessage()));
        }
        return Optional.empty();
    }

    public static Optional<List<RequestHistoryResponse>> getRequestHistoryByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            String url = BASE_URL + "/date-range?startDate=" + startDate.toString() + "&endDate=" + endDate.toString();
            BaseResponse<List<RequestHistoryResponse>> response = ApiClient.get(url,
                    new TypeReference<BaseResponse<List<RequestHistoryResponse>>>() {
                    });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to get request history by date range: " + e.getMessage()));
        }
        return Optional.empty();
    }

    public static Optional<Long> getUserHistoryCount() {
        try {
            BaseResponse<Long> response = ApiClient.get(BASE_URL + "/count",
                    new TypeReference<BaseResponse<Long>>() {
                    });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to get history count: " + e.getMessage()));
        }
        return Optional.empty();
    }

    public static boolean clearUserHistory() {
        try {
            BaseResponse<Void> response = ApiClient.delete(BASE_URL,
                    new TypeReference<BaseResponse<Void>>() {
                    });
            return response != null && response.isSuccess();
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to clear history: " + e.getMessage()));
            return false;
        }
    }

    public static boolean clearRequestHistory(Long requestId) {
        try {
            BaseResponse<Void> response = ApiClient.delete(BASE_URL + "/request/" + requestId,
                    new TypeReference<BaseResponse<Void>>() {
                    });
            return response != null && response.isSuccess();
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to clear request history: " + e.getMessage()));
            return false;
        }
    }

    public static boolean clearOldHistory(LocalDateTime beforeDate) {
        try {
            BaseResponse<Void> response = ApiClient.delete(BASE_URL + "/old?beforeDate=" + beforeDate.toString(),
                    new TypeReference<BaseResponse<Void>>() {
                    });
            return response != null && response.isSuccess();
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to clear old history: " + e.getMessage()));
            return false;
        }
    }

    public static Optional<RequestHistoryResponse> getHistoryById(Long historyId) {
        try {
            BaseResponse<RequestHistoryResponse> response = ApiClient.get(BASE_URL + "/" + historyId,
                    new TypeReference<BaseResponse<RequestHistoryResponse>>() {
                    });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to get history item: " + e.getMessage()));
        }
        return Optional.empty();
    }
}