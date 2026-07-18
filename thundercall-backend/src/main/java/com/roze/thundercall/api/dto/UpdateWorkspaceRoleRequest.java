package com.roze.thundercall.api.dto;

import com.roze.thundercall.api.enums.WorkspaceRole;
import jakarta.validation.constraints.NotNull;

public record UpdateWorkspaceRoleRequest(
        @NotNull WorkspaceRole role
) {
}
