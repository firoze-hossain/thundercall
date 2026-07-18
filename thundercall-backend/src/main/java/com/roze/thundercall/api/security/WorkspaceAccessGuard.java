package com.roze.thundercall.api.security;

import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.entity.Workspace;
import com.roze.thundercall.api.entity.WorkspaceAccess;
import com.roze.thundercall.api.enums.WorkspaceRole;
import com.roze.thundercall.api.exception.AuthException;
import com.roze.thundercall.api.exception.ResourceNotFoundException;
import com.roze.thundercall.api.repository.WorkspaceAccessRepository;
import com.roze.thundercall.api.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** The one place that decides "can this user touch this workspace's
 * stuff" — used by Collection/Folder/Request/Environment services so
 * an Editor with shared access can do everything a normal owner can,
 * without every one of those services re-implementing the same
 * owner-or-shared-access check slightly differently. Viewers can read
 * but never write; owners can always do both, with no access row
 * needed for themselves. */
@Component
@RequiredArgsConstructor
public class WorkspaceAccessGuard {
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceAccessRepository workspaceAccessRepository;

    /** For create operations that take an explicit workspaceId — owns
     * it, or has Editor access to it. Throws otherwise. */
    public Workspace resolveForWrite(Long workspaceId, User user) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
        requireWrite(workspace, user);
        return workspace;
    }

    /** For create operations — owns it, or has at least Viewer access
     * (read-only callers use this; write callers use resolveForWrite). */
    public Workspace resolveForRead(Long workspaceId, User user) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
        requireRead(workspace, user);
        return workspace;
    }

    /** For update/delete on an entity you already fetched by plain ID
     * (bypassing the old owner-scoped repository lookup) — call this
     * with that entity's workspace before proceeding. */
    public void requireWrite(Workspace workspace, User user) {
        if (isOwner(workspace, user)) {
            return;
        }
        WorkspaceAccess access = accessOf(workspace, user)
                .orElseThrow(() -> new AuthException("You don't have access to this workspace"));
        if (access.getRole() != WorkspaceRole.EDITOR) {
            throw new AuthException("Editor access is required to make changes in this workspace");
        }
    }

    public void requireRead(Workspace workspace, User user) {
        if (isOwner(workspace, user)) {
            return;
        }
        accessOf(workspace, user)
                .orElseThrow(() -> new AuthException("You don't have access to this workspace"));
    }

    public boolean isOwner(Workspace workspace, User user) {
        return workspace.getOwner().getId().equals(user.getId());
    }

    private Optional<WorkspaceAccess> accessOf(Workspace workspace, User user) {
        return workspaceAccessRepository.findByWorkspaceAndUser(workspace, user);
    }
}
