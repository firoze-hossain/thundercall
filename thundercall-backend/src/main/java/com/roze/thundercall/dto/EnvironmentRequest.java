package com.roze.thundercall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record EnvironmentRequest(
    @NotBlank(message = "Environment name is required")
    String name,
    String description,
    @NotNull
    Map<String, String> variables,
    Boolean isActive
) {
    public EnvironmentRequest {
        if (isActive == null) {
            isActive = true;
        }
    }
}