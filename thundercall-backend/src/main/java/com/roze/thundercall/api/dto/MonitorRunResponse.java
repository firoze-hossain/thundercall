package com.roze.thundercall.api.dto;

import java.time.LocalDateTime;

public record MonitorRunResponse(
        Long id,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        int totalRequests,
        int passedRequests,
        int failedRequests,
        long avgResponseTimeMs,
        boolean success,
        String details
) {
}