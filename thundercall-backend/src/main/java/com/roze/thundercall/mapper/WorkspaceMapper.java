package com.roze.thundercall.mapper;

import com.roze.thundercall.dto.WorkspaceResponse;
import com.roze.thundercall.entity.Workspace;
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
