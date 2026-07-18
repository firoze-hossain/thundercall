package com.roze.thundercall.api.dto;

import com.roze.thundercall.api.enums.WorkspaceRole;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record InviteTeamRequest(
        @NotNull Long teamId,
        @NotNull WorkspaceRole role,
        List<Long> environmentIds
) {
}
