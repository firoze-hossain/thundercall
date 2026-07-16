package com.roze.thundercall.api.dto;

import com.roze.thundercall.api.enums.TeamRole;

import java.time.LocalDateTime;

public record TeamMemberResponse(
        Long id,
        Long userId,
        String username,
        String email,
        TeamRole role,
        LocalDateTime joinedAt
) {
}
