package com.roze.thundercall.mapper;

import com.roze.thundercall.dto.FolderRequest;
import com.roze.thundercall.dto.FolderResponse;
import com.roze.thundercall.entity.Folder;
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
                .collectionName(folder.getCollection().getName())
                .requestCount(folder.getRequests() != null ? folder.getRequests().size() : 0)
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .build();
    }
}
