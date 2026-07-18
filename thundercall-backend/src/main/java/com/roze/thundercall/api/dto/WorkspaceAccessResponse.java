package com.roze.thundercall.api.dto;

import com.roze.thundercall.api.enums.WorkspaceRole;

import java.time.LocalDateTime;
import java.util.List;

public record WorkspaceAccessResponse(
        Long id,
        Long workspaceId,
        String workspaceName,
        String workspaceOwnerUsername,
        Long userId,
        String username,
        String email,
        Long teamId,
        String teamName,
        WorkspaceRole role,
        List<Long> allowedEnvironmentIds,
        List<String> allowedEnvironmentNames,
        LocalDateTime grantedAt
) {
}
