package com.roze.thundercall.api.dto;

import com.roze.thundercall.api.enums.HttpMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MockRouteRequest(
        @NotNull HttpMethod method,
        @NotBlank String path,
        @NotNull Integer responseStatus,
        String responseBody,
        String responseHeaders,
        Integer delayMs
) {
}