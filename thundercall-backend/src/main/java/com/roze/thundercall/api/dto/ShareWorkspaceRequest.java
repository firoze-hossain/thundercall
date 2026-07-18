package com.roze.thundercall.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ShareWorkspaceRequest(
        @NotNull Long teamId,
        @NotEmpty @Valid List<MemberRoleEntry> members
) {
}
