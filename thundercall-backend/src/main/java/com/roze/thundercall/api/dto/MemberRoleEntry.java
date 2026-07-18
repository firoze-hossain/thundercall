package com.roze.thundercall.api.dto;

import com.roze.thundercall.api.enums.WorkspaceRole;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record MemberRoleEntry(
        @NotNull Long userId,
        @NotNull WorkspaceRole role,
        // Explicit opt-in — which of the workspace's environments this
        // member should see. Empty/null means none, not "all" — a
        // member with workspace access sees zero environments until
        // specifically granted some.
        List<Long> environmentIds
) {
}
