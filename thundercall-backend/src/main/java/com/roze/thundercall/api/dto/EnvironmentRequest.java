package com.roze.thundercall.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/** FIX: workspaceId added, mirroring CollectionRequest — without it,
 * environments could only ever be created in the default workspace,
 * with no way to create one in a specific workspace (including a
 * shared one an Editor is currently working in). Null falls back to
 * the default workspace, same as before. */
public record EnvironmentRequest(
    @NotBlank(message = "Environment name is required")
    String name,
    String description,
    @NotNull
    Map<String, String> variables,
    Boolean isActive,
    Long workspaceId
) {
    public EnvironmentRequest {
        if (isActive == null) {
            isActive = true;
        }
    }
}