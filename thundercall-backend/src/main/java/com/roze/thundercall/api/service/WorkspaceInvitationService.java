package com.roze.thundercall.api.service;

import com.roze.thundercall.api.dto.InviteToWorkspaceRequest;
import com.roze.thundercall.api.dto.WorkspaceAccessResponse;
import com.roze.thundercall.api.dto.WorkspaceInvitationResponse;
import com.roze.thundercall.api.entity.User;

import java.util.List;

/** The simple, direct way to share a workspace — invite someone by
 * email, no team required at all. This is the primary, recommended
 * path; WorkspaceSharingService's team-based sharing still exists for
 * anyone who wants to share with a whole team's members at once. */
public interface WorkspaceInvitationService {
    WorkspaceInvitationResponse inviteToWorkspace(Long workspaceId, InviteToWorkspaceRequest request, User owner);

    /** The bulk convenience action — invites every member of a team at
     * once instead of one email at a time. Under the hood this creates
     * the exact same kind of invitation as inviteToWorkspace() for each
     * member (skipping anyone who already has access or a pending
     * invite) — one consistent invite/accept path for everyone,
     * whether they were invited individually or as part of a team. */
    List<WorkspaceInvitationResponse> inviteTeamMembers(Long workspaceId, com.roze.thundercall.api.dto.InviteTeamRequest request, User owner);

    List<WorkspaceInvitationResponse> getPendingInvitations(Long workspaceId, User owner);

    void revokeInvitation(Long workspaceId, Long invitationId, User owner);

    /** Returns the resulting access grant so the UI can show the
     * accepting user exactly what they just got. */
    WorkspaceAccessResponse acceptInvitation(String token, User acceptingUser);
}
