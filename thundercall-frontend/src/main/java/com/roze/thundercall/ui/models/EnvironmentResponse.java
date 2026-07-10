package com.roze.thundercall.ui.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EnvironmentResponse {
    private Long id;
    private String name;
    private String description;
    private Map<String, String> variables;
    private Long workspaceId;
    private String workspaceName;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}