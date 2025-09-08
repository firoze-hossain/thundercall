package com.roze.thundercall.mapper;

import com.roze.thundercall.dto.CollectionRequest;
import com.roze.thundercall.dto.CollectionResponse;
import com.roze.thundercall.dto.FolderResponse;
import com.roze.thundercall.dto.RequestResponse;
import com.roze.thundercall.entity.Collection;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CollectionMapper {
    public Collection toEntity(CollectionRequest request) {
        Collection collection = Collection.builder()
                .name(request.name())
                .description(request.description())
                .build();
        return collection;
    }

    public CollectionResponse toShortResponse(Collection collection) {
        return CollectionResponse.builder()
                .id(collection.getId())
                .name(collection.getName())
                .description(collection.getDescription())
                .workspaceId(collection.getWorkspace().getId())
                .workspaceName(collection.getWorkspace().getName())
                .requestCount(collection.getRequests().size())
                .createdAt(collection.getCreatedAt())
                .updatedAt(collection.getUpdatedAt())
                .build();
    }

    public CollectionResponse toResponse(Collection collection) {
        List<RequestResponse> requestResponses = collection.getRequests().stream()
                .map(request -> RequestResponse.builder()
                        .id(request.getId())
                        .name(request.getName())
                        .description(request.getDescription())
                        .method(request.getMethod())
                        .url(request.getUrl())
                        .headers(request.getHeaders())
                        .body(request.getBody())
                        .collectionId(collection.getId())
                        .collectionName(collection.getName())
                        .folderId(request.getFolder() != null ? request.getFolder().getId() : null)
                        .folderName(request.getFolder() != null ? request.getFolder().getName() : null)
                        .createdAt(request.getCreatedAt())
                        .updatedAt(request.getUpdatedAt())
                        .build()).toList();
        return CollectionResponse.builder()
                .id(collection.getId())
                .name(collection.getName())
                .description(collection.getDescription())
                .workspaceId(collection.getWorkspace().getId())
                .workspaceName(collection.getWorkspace().getName())
                .requestCount(collection.getRequests().size())
                .createdAt(collection.getCreatedAt())
                .updatedAt(collection.getUpdatedAt())
                .requests(requestResponses)
                .build();
    }

    public CollectionResponse toDetailedResponse(Collection collection, List<FolderResponse> folderResponses, List<RequestResponse> requestResponses) {
        return CollectionResponse.builder()
                .id(collection.getId())
                .name(collection.getName())
                .description(collection.getDescription())
                .workspaceId(collection.getWorkspace().getId())
                .workspaceName(collection.getWorkspace().getName())
                .requestCount(requestResponses.size())
                .folderCount(folderResponses.size())
                .createdAt(collection.getCreatedAt())
                .updatedAt(collection.getUpdatedAt())
                .requests(requestResponses)
                .folderResponses(folderResponses)
                .build();
    }
}
