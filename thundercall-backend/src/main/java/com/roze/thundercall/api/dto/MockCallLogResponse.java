package com.roze.thundercall.api.dto;

import java.time.LocalDateTime;

public record MockCallLogResponse(
        Long id,
        Long mockServerId,
        LocalDateTime timestamp,
        String method,
        String path,
        String queryParams,
        String requestHeaders,
        String requestBody,
        int responseStatus,
        String responseHeaders,
        String responseBody,
        long durationMs,
        boolean matched,
        String matchedRoutePattern
) {}
