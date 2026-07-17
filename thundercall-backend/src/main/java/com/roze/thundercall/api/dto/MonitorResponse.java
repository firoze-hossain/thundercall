package com.roze.thundercall.api.dto;

import java.time.LocalDateTime;

public record MonitorResponse(
        Long id,
        String name,
        Long collectionId,
        String collectionName,
        Long environmentId,
        String environmentName,
        int intervalMinutes,
        boolean enabled,
        boolean notifyOnFailure,
        String notifyEmail,
        LocalDateTime lastRunAt,
        String lastRunStatus,
        LocalDateTime createdAt
) {
}