package com.roze.thundercall.ui.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkspaceAccessResponse {
    private Long id;
    private Long workspaceId;
    private String workspaceName;
    private String workspaceOwnerUsername;
    private Long userId;
    private String username;
    private String email;
    private Long teamId;
    private String teamName;
    private String role;
    private List<Long> allowedEnvironmentIds;
    private List<String> allowedEnvironmentNames;
    private String grantedAt;
}
