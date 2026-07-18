package com.roze.thundercall.api.dto;

import com.roze.thundercall.api.enums.InvitationStatus;
import com.roze.thundercall.api.enums.WorkspaceRole;

import java.time.LocalDateTime;

public record WorkspaceInvitationResponse(
        Long id,
        Long workspaceId,
        String workspaceName,
        String email,
        WorkspaceRole role,
        InvitationStatus status,
        String invitedByUsername,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
}
