package com.roze.thundercall.ui.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.roze.thundercall.ui.models.*;
import com.roze.thundercall.ui.utils.AlertUtils;
import javafx.application.Platform;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class WorkspaceSharingService {
    private static final String BASE_URL = "/workspace-sharing";

    /** Throws so the caller can show the specific reason (not a team
     * owner/admin, member not on that team, etc.) rather than a generic
     * failure message. */
    public static List<WorkspaceAccessResponse> shareWorkspace(Long workspaceId, ShareWorkspaceRequest request) throws IOException {
        BaseResponse<List<WorkspaceAccessResponse>> response = ApiClient.post(
                BASE_URL + "/" + workspaceId + "/share", request,
                new TypeReference<BaseResponse<List<WorkspaceAccessResponse>>>() {
                });
        if (response == null || !response.isSuccess()) {
            throw new IOException("Invalid response from server");
        }
        return response.getData();
    }

    public static Optional<List<WorkspaceAccessResponse>> getSharedWithMe() {
        try {
            BaseResponse<List<WorkspaceAccessResponse>> response = ApiClient.get(BASE_URL + "/shared-with-me",
                    new TypeReference<BaseResponse<List<WorkspaceAccessResponse>>>() {
                    });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to load shared workspaces: " + friendlyMessage(e)));
        }
        return Optional.empty();
    }

    public static Optional<List<WorkspaceAccessResponse>> getWorkspaceAccessList(Long workspaceId) {
        try {
            BaseResponse<List<WorkspaceAccessResponse>> response = ApiClient.get(BASE_URL + "/" + workspaceId + "/access",
                    new TypeReference<BaseResponse<List<WorkspaceAccessResponse>>>() {
                    });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to load access list: " + friendlyMessage(e)));
        }
        return Optional.empty();
    }

    public static boolean updateAccessRole(Long workspaceId, Long userId, String role) {
        try {
            ApiClient.put(BASE_URL + "/" + workspaceId + "/access/" + userId,
                    new UpdateWorkspaceRoleRequest(role), new TypeReference<BaseResponse<WorkspaceAccessResponse>>() {
                    });
            return true;
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to update role: " + friendlyMessage(e)));
            return false;
        }
    }

    public static boolean updateAccessEnvironments(Long workspaceId, Long userId, List<Long> environmentIds) {
        try {
            ApiClient.put(BASE_URL + "/" + workspaceId + "/access/" + userId + "/environments", environmentIds,
                    new TypeReference<BaseResponse<WorkspaceAccessResponse>>() {
                    });
            return true;
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to update environments: " + friendlyMessage(e)));
            return false;
        }
    }

    public static boolean revokeAccess(Long workspaceId, Long userId) {
        try {
            BaseResponse<Void> response = ApiClient.delete(BASE_URL + "/" + workspaceId + "/access/" + userId,
                    new TypeReference<BaseResponse<Void>>() {
                    });
            return response != null && response.isSuccess();
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to revoke access: " + friendlyMessage(e)));
            return false;
        }
    }

    public static Optional<WorkspaceContentsResponse> getWorkspaceContents(Long workspaceId) {
        try {
            BaseResponse<WorkspaceContentsResponse> response = ApiClient.get(BASE_URL + "/" + workspaceId + "/contents",
                    new TypeReference<BaseResponse<WorkspaceContentsResponse>>() {
                    });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to load workspace: " + friendlyMessage(e)));
        }
        return Optional.empty();
    }

    /** Throws so the caller can show exactly what went wrong (no
     * access, request no longer exists, the target server itself
     * failed) rather than a generic message. overrides may be null to
     * send the request exactly as the owner saved it. */
    public static ApiResponse executeSharedRequest(Long workspaceId, Long requestId, ApiRequest overrides) throws IOException {
        BaseResponse<ApiResponse> response = ApiClient.post(
                BASE_URL + "/" + workspaceId + "/requests/" + requestId + "/execute", overrides,
                new TypeReference<BaseResponse<ApiResponse>>() {
                });
        if (response == null || !response.isSuccess()) {
            throw new IOException("Invalid response from server");
        }
        return response.getData();
    }

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
