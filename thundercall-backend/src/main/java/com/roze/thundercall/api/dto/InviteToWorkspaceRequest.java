package com.roze.thundercall.api.dto;

import com.roze.thundercall.api.enums.WorkspaceRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record InviteToWorkspaceRequest(
        @NotBlank String email,
        @NotNull WorkspaceRole role,
        // Explicit opt-in — which environments they should see once they
        // accept. Empty/null means none, not "all".
        List<Long> environmentIds
) {
}
