package com.roze.thundercall.api.dto;

import java.time.LocalDateTime;

public record MockServerResponse(
        Long id,
        String name,
        String description,
        boolean enabled,
        // Full, ready-to-hit base URL for this mock server — the frontend
        // doesn't need to know or guess the routing scheme.
        String baseUrl,
        long routeCount,
        LocalDateTime createdAt
) {
}