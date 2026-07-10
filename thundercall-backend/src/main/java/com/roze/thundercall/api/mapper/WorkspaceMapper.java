package com.roze.thundercall.api.mapper;

import com.roze.thundercall.api.dto.WorkspaceResponse;
import com.roze.thundercall.api.entity.Workspace;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceMapper {
    public WorkspaceResponse toResponse(Workspace workspace) {
        return WorkspaceResponse.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .description(workspace.getDescription())
                .collectionCount(workspace.getCollections().size())
                .createdAt(workspace.getCreatedAt())
                .build();
    }
}
