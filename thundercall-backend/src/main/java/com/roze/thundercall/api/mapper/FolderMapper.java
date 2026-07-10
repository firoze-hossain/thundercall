package com.roze.thundercall.api.mapper;

import com.roze.thundercall.api.dto.FolderRequest;
import com.roze.thundercall.api.dto.FolderResponse;
import com.roze.thundercall.api.entity.Folder;
import org.springframework.stereotype.Component;

@Component
public class FolderMapper {

    public Folder toEntity(FolderRequest request) {
        return Folder.builder()
                .name(request.name())
                .description(request.description())
                .build();
    }

    public FolderResponse toResponse(Folder folder) {
        return FolderResponse.builder()
                .id(folder.getId())
                .name(folder.getName())
                .description(folder.getDescription())
                .collectionId(folder.getCollection().getId())
                .parentFolderId(folder.getParentFolder() != null ? folder.getParentFolder().getId() : null)
                .collectionName(folder.getCollection().getName())
                .requestCount(folder.getRequests() != null ? folder.getRequests().size() : 0)
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .build();
    }
}