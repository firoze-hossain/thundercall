package com.roze.thundercall.api.service;

import com.roze.thundercall.api.dto.ShareWorkspaceRequest;
import com.roze.thundercall.api.dto.UpdateWorkspaceRoleRequest;
import com.roze.thundercall.api.dto.WorkspaceAccessResponse;
import com.roze.thundercall.api.dto.WorkspaceContentsResponse;
import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.enums.WorkspaceRole;

import java.util.List;

public interface WorkspaceSharingService {
    /** Shares the caller's own workspace with one or more members of a
     * team they're an Owner/Admin of — each named member must already
     * be on that team, and gets an explicit role for this workspace. */
    List<WorkspaceAccessResponse> shareWorkspace(Long workspaceId, ShareWorkspaceRequest request, User owner);

    /** Workspaces shared WITH the current user — their "Team Workspaces" list. */
    List<WorkspaceAccessResponse> getSharedWithMe(User user);

    /** Who currently has access to a workspace the caller owns. */
    List<WorkspaceAccessResponse> getWorkspaceAccessList(Long workspaceId, User owner);

    WorkspaceAccessResponse updateAccessRole(Long workspaceId, Long userId, UpdateWorkspaceRoleRequest request, User owner);

    /** Changes which environments an existing member can see, without
     * touching their role or requiring a remove-and-reinvite round
     * trip. Same opt-in rule as everywhere else — an empty list means
     * none, not "leave unchanged" or "all". */
    WorkspaceAccessResponse updateAccessEnvironments(Long workspaceId, Long userId, List<Long> environmentIds, User owner);

    void revokeAccess(Long workspaceId, Long userId, User owner);

    /** True if the user owns the workspace OR has been granted at least
     * the given role (EDITOR implies EDITOR-or-better; VIEWER accepts
     * either role) — the one shared check every shared-workspace data
     * endpoint should run before doing anything else. */
    boolean hasAccess(Long workspaceId, User user, WorkspaceRole minimumRole);

    /** Full read-only contents of a shared workspace — collections
     * (with folders and requests) and environments — for browsing a
     * workspace you don't own. Throws if the caller has no access at
     * all (not even Viewer). */
    WorkspaceContentsResponse getWorkspaceContents(Long workspaceId, User user);

    /** Actually sends a request that lives in someone else's shared
     * workspace — Editor role required. Doesn't touch the caller's own
     * Request History (there's no clean owner for that entry — it's
     * not the caller's request, and attributing it to the workspace
     * owner would be misleading too), so this is a lean, direct HTTP
     * call rather than routing through RequestService.executeRequest(). */
    com.roze.thundercall.api.dto.ApiResponse executeSharedRequest(
            Long workspaceId, Long requestId, com.roze.thundercall.api.dto.ApiRequest overrides, User user);
}
