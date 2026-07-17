package com.roze.thundercall.api.dto;

import com.roze.thundercall.api.enums.HttpMethod;

import java.time.LocalDateTime;

public record MockRouteResponse(
        Long id,
        Long mockServerId,
        HttpMethod method,
        String path,
        int responseStatus,
        String responseBody,
        String responseHeaders,
        int delayMs,
        LocalDateTime createdAt
) {
}