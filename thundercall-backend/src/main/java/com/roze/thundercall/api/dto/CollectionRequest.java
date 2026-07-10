package com.roze.thundercall.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * FIX: workspaceId added so collections are created in the workspace the
 * user has SELECTED in the UI. Previously the backend always used the
 * user's first workspace — collections created while another workspace was
 * active landed in a hidden workspace and never appeared in the sidebar.
 * Null workspaceId falls back to the default workspace (old behaviour).
 */
public record CollectionRequest(
        @NotBlank(message = "Collection name is required")
        String name,
        String description,
        Long workspaceId
) {
}