package com.roze.thundercall.ui.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.roze.thundercall.ui.models.*;
import com.roze.thundercall.ui.utils.AlertUtils;
import javafx.application.Platform;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class TeamService {
    private static final String BASE_URL = "/teams";

    public static Optional<TeamResponse> createTeam(String name, String description) {
        try {
            TeamRequest request = new TeamRequest(name, description);
            BaseResponse<TeamResponse> response = ApiClient.post(BASE_URL, request,
                    new TypeReference<BaseResponse<TeamResponse>>() {
                    });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to create team: " + friendlyMessage(e)));
        }
        return Optional.empty();
    }

    public static Optional<List<TeamResponse>> getMyTeams() {
        try {
            BaseResponse<List<TeamResponse>> response = ApiClient.get(BASE_URL,
                    new TypeReference<BaseResponse<List<TeamResponse>>>() {
                    });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to load teams: " + friendlyMessage(e)));
        }
        return Optional.empty();
    }

    public static Optional<TeamResponse> getTeam(Long teamId) {
        try {
            BaseResponse<TeamResponse> response = ApiClient.get(BASE_URL + "/" + teamId,
                    new TypeReference<BaseResponse<TeamResponse>>() {
                    });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to load team: " + friendlyMessage(e)));
        }
        return Optional.empty();
    }

    public static Optional<List<TeamMemberResponse>> getMembers(Long teamId) {
        try {
            BaseResponse<List<TeamMemberResponse>> response = ApiClient.get(BASE_URL + "/" + teamId + "/members",
                    new TypeReference<BaseResponse<List<TeamMemberResponse>>>() {
                    });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to load members: " + friendlyMessage(e)));
        }
        return Optional.empty();
    }

    /** Returns the resulting invitation on success, or empty on failure —
     * the caller is expected to show the IOException's message itself
     * since the reasons here (already a member, pending invite exists,
     * mail not configured...) are specific and worth surfacing exactly. */
    public static Optional<TeamInvitationResponse> inviteMember(Long teamId, String email, String role) throws IOException {
        InviteMemberRequest request = new InviteMemberRequest(email, role);
        BaseResponse<TeamInvitationResponse> response = ApiClient.post(BASE_URL + "/" + teamId + "/invitations", request,
                new TypeReference<BaseResponse<TeamInvitationResponse>>() {
                });
        return response != null && response.isSuccess() ? Optional.ofNullable(response.getData()) : Optional.empty();
    }

    public static Optional<List<TeamInvitationResponse>> getPendingInvitations(Long teamId) {
        try {
            BaseResponse<List<TeamInvitationResponse>> response = ApiClient.get(
                    BASE_URL + "/" + teamId + "/invitations",
                    new TypeReference<BaseResponse<List<TeamInvitationResponse>>>() {
                    });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to load invitations: " + friendlyMessage(e)));
        }
        return Optional.empty();
    }

    public static boolean revokeInvitation(Long teamId, Long invitationId) {
        try {
            BaseResponse<Void> response = ApiClient.delete(BASE_URL + "/" + teamId + "/invitations/" + invitationId,
                    new TypeReference<BaseResponse<Void>>() {
                    });
            return response != null && response.isSuccess();
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to revoke invitation: " + friendlyMessage(e)));
            return false;
        }
    }

    /** Throws so the caller can show the SPECIFIC reason (wrong email,
     * expired, already used...) rather than a generic failure message. */
    public static TeamResponse acceptInvitation(String token) throws IOException {
        AcceptInvitationRequest request = new AcceptInvitationRequest(token);
        BaseResponse<TeamResponse> response = ApiClient.post(BASE_URL + "/invitations/accept", request,
                new TypeReference<BaseResponse<TeamResponse>>() {
                });
        if (response == null || !response.isSuccess()) {
            throw new IOException("Invalid response from server");
        }
        return response.getData();
    }

    public static boolean removeMember(Long teamId, Long memberUserId) {
        try {
            BaseResponse<Void> response = ApiClient.delete(BASE_URL + "/" + teamId + "/members/" + memberUserId,
                    new TypeReference<BaseResponse<Void>>() {
                    });
            return response != null && response.isSuccess();
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to remove member: " + friendlyMessage(e)));
            return false;
        }
    }

    public static boolean changeMemberRole(Long teamId, Long memberUserId, String newRole) {
        try {
            ChangeMemberRoleRequest request = new ChangeMemberRoleRequest(newRole);
            ApiClient.put(BASE_URL + "/" + teamId + "/members/" + memberUserId + "/role", request,
                    new TypeReference<BaseResponse<Void>>() {
                    });
            return true;
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to change role: " + friendlyMessage(e)));
            return false;
        }
    }

    public static boolean leaveTeam(Long teamId) {
        try {
            ApiClient.post(BASE_URL + "/" + teamId + "/leave", null, new TypeReference<BaseResponse<Void>>() {
            });
            return true;
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to leave team: " + friendlyMessage(e)));
            return false;
        }
    }

    public static boolean deleteTeam(Long teamId) {
        try {
            BaseResponse<Void> response = ApiClient.delete(BASE_URL + "/" + teamId,
                    new TypeReference<BaseResponse<Void>>() {
                    });
            return response != null && response.isSuccess();
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to delete team: " + friendlyMessage(e)));
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