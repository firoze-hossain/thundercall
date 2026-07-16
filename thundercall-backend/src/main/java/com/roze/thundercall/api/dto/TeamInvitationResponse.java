package com.roze.thundercall.api.dto;

import com.roze.thundercall.api.enums.InvitationStatus;
import com.roze.thundercall.api.enums.TeamRole;

import java.time.LocalDateTime;

public record TeamInvitationResponse(
        Long id,
        Long teamId,
        String teamName,
        String email,
        TeamRole role,
        InvitationStatus status,
        String invitedByUsername,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
}
