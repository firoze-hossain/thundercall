package com.roze.thundercall.ui.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.roze.thundercall.ui.models.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/** The simple, direct way to share a workspace — invite by email, no
 * team required. See WorkspaceSharingService for the team-based
 * alternative. */
public class WorkspaceInvitationService {
    private static final String BASE_URL = "/workspace-invitations";

    /** Throws so the caller can show the specific reason (already has
     * access, already invited, mail not configured...) rather than a
     * generic failure message. */
    public static WorkspaceInvitationResponse inviteToWorkspace(Long workspaceId, InviteToWorkspaceRequest request) throws IOException {
        BaseResponse<WorkspaceInvitationResponse> response = ApiClient.post(
                BASE_URL + "/" + workspaceId + "/invite", request,
                new TypeReference<BaseResponse<WorkspaceInvitationResponse>>() {
                });
        if (response == null || !response.isSuccess()) {
            throw new IOException("Invalid response from server");
        }
        return response.getData();
    }

    /** Bulk convenience — invites every member of a team at once.
     * Individual members who already have access or a pending invite
     * are silently skipped rather than erroring the whole batch, so
     * this is safe to call repeatedly (e.g. after adding someone new
     * to the team) without duplicate invites piling up. */
    public static List<WorkspaceInvitationResponse> inviteTeamMembers(Long workspaceId, InviteTeamRequest request) throws IOException {
        BaseResponse<List<WorkspaceInvitationResponse>> response = ApiClient.post(
                BASE_URL + "/" + workspaceId + "/invite-team", request,
                new TypeReference<BaseResponse<List<WorkspaceInvitationResponse>>>() {
                });
        if (response == null || !response.isSuccess()) {
            throw new IOException("Invalid response from server");
        }
        return response.getData();
    }

    public static Optional<List<WorkspaceInvitationResponse>> getPendingInvitations(Long workspaceId) {
        try {
            BaseResponse<List<WorkspaceInvitationResponse>> response = ApiClient.get(
                    BASE_URL + "/" + workspaceId + "/pending",
                    new TypeReference<BaseResponse<List<WorkspaceInvitationResponse>>>() {
                    });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException ignored) {
            // shown inline by the caller instead — a missing pending list
            // isn't worth an alert popup on its own
        }
        return Optional.empty();
    }

    public static boolean revokeInvitation(Long workspaceId, Long invitationId) {
        try {
            BaseResponse<Void> response = ApiClient.delete(BASE_URL + "/" + workspaceId + "/" + invitationId,
                    new TypeReference<BaseResponse<Void>>() {
                    });
            return response != null && response.isSuccess();
        } catch (IOException e) {
            return false;
        }
    }

    /** Throws so the caller can show exactly why acceptance failed
     * (expired, wrong email, already a member, etc). */
    public static WorkspaceAccessResponse acceptInvitation(String token) throws IOException {
        BaseResponse<WorkspaceAccessResponse> response = ApiClient.post(BASE_URL + "/accept",
                new AcceptInvitationRequest(token), new TypeReference<BaseResponse<WorkspaceAccessResponse>>() {
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
