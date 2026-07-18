package com.roze.thundercall.ui.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkspaceContentsResponse {
    private Long workspaceId;
    private String workspaceName;
    private String ownerUsername;
    private List<CollectionResponse> collections;
    private List<EnvironmentResponse> environments;
}
