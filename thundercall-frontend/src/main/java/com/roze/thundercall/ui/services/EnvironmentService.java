package com.roze.thundercall.ui.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.roze.thundercall.ui.models.BaseResponse;
import com.roze.thundercall.ui.models.EnvironmentRequest;
import com.roze.thundercall.ui.models.EnvironmentResponse;
import com.roze.thundercall.ui.utils.AlertUtils;
import javafx.application.Platform;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EnvironmentService {
    private static final String BASE_URL = "/environments";

    public static Optional<EnvironmentResponse> createEnvironment(String name, String description, Map<String, String> variables) {
        try {
            EnvironmentRequest request = new EnvironmentRequest(name, description, variables, true);
            BaseResponse<EnvironmentResponse> response = ApiClient.post(BASE_URL, request, new TypeReference<BaseResponse<EnvironmentResponse>>() {
            });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to create environment: " + e.getMessage()));
        }
        return Optional.empty();
    }

    public static Optional<List<EnvironmentResponse>> getUserEnvironments() {
        try {
            BaseResponse<List<EnvironmentResponse>> response = ApiClient.get(BASE_URL, new TypeReference<BaseResponse<List<EnvironmentResponse>>>() {
            });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to get environments: " + e.getMessage()));
        }
        return Optional.empty();
    }

    public static Optional<List<EnvironmentResponse>> getActiveEnvironments() {
        try {
            BaseResponse<List<EnvironmentResponse>> response = ApiClient.get(BASE_URL + "/active", new TypeReference<BaseResponse<List<EnvironmentResponse>>>() {
            });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to get active environments: " + e.getMessage()));
        }
        return Optional.empty();
    }

    public static Optional<EnvironmentResponse> getEnvironmentById(Long id) {
        try {
            BaseResponse<EnvironmentResponse> response = ApiClient.get(BASE_URL + "/" + id, new TypeReference<BaseResponse<EnvironmentResponse>>() {
            });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to get environment: " + e.getMessage()));
        }
        return Optional.empty();
    }

    public static Optional<EnvironmentResponse> updateEnvironment(Long id, String name, String description, Map<String, String> variables, Boolean isActive) {
        try {
            EnvironmentRequest request = new EnvironmentRequest(name, description, variables, isActive);
            BaseResponse<EnvironmentResponse> response = ApiClient.put(BASE_URL + "/" + id, request, new TypeReference<BaseResponse<EnvironmentResponse>>() {
            });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to update environment: " + e.getMessage()));
        }
        return Optional.empty();
    }

    public static Optional<EnvironmentResponse> updateEnvironmentVariables(Long id, Map<String, String> variables) {
        try {
            // First get the current environment
            Optional<EnvironmentResponse> currentEnv = getEnvironmentById(id);
            if (currentEnv.isPresent()) {
                EnvironmentResponse env = currentEnv.get();

                // Merge the new variables with existing ones
                Map<String, String> mergedVariables = new HashMap<>(env.getVariables());
                mergedVariables.putAll(variables);

                // Update the environment
                EnvironmentRequest request = new EnvironmentRequest(
                        env.getName(),
                        env.getDescription(),
                        mergedVariables,
                        env.getIsActive()
                );

                BaseResponse<EnvironmentResponse> response = ApiClient.put(BASE_URL + "/" + id, request,
                        new TypeReference<BaseResponse<EnvironmentResponse>>() {
                        });

                if (response != null && response.isSuccess()) {
                    return Optional.ofNullable(response.getData());
                }
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to update environment variables: " + e.getMessage()));
        }
        return Optional.empty();
    }

    public static Optional<EnvironmentResponse> toggleEnvironmentStatus(Long id, Boolean active) {
        try {
            BaseResponse<EnvironmentResponse> response = ApiClient.patch(BASE_URL + "/" + id + "/status?active=" + active, null, new TypeReference<BaseResponse<EnvironmentResponse>>() {
            });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to toggle environment status: " + e.getMessage()));
        }
        return Optional.empty();
    }

    public static boolean deleteEnvironment(Long id) {
        try {
            BaseResponse<Void> response = ApiClient.delete(BASE_URL + "/" + id, new TypeReference<BaseResponse<Void>>() {
            });
            return response != null && response.isSuccess();
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to delete environment: " + e.getMessage()));
            return false;
        }
    }

}