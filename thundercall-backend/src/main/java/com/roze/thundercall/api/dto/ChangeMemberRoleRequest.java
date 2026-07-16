package com.roze.thundercall.api.dto;

import com.roze.thundercall.api.enums.TeamRole;
import jakarta.validation.constraints.NotNull;

public record ChangeMemberRoleRequest(
        @NotNull TeamRole role
) {
}
