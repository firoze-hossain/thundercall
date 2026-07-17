package com.roze.thundercall.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MonitorRequest(
        @NotBlank String name,
        @NotNull Long collectionId,
        Long environmentId,
        @Min(1) int intervalMinutes,
        boolean enabled,
        boolean notifyOnFailure,
        String notifyEmail
) {
}