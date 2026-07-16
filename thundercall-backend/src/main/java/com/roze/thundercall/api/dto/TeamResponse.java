package com.roze.thundercall.api.dto;

import com.roze.thundercall.api.enums.TeamRole;

import java.time.LocalDateTime;

public record TeamResponse(
        Long id,
        String name,
        String description,
        String ownerUsername,
        String ownerEmail,
        long memberCount,
        TeamRole myRole,
        LocalDateTime createdAt
) {
}
