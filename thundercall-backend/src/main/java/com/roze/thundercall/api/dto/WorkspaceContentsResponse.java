package com.roze.thundercall.api.dto;

import java.util.List;

public record WorkspaceContentsResponse(
        Long workspaceId,
        String workspaceName,
        String ownerUsername,
        List<CollectionResponse> collections,
        List<EnvironmentResponse> environments
) {
}
