package com.roze.thundercall.api.mapper;

import com.roze.thundercall.api.dto.ApiRequest;
import com.roze.thundercall.api.dto.RequestResponse;
import com.roze.thundercall.api.entity.Request;
import com.roze.thundercall.api.repository.CollectionRepository;
import com.roze.thundercall.api.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RequestMapper {
    private final CollectionRepository collectionRepository;
    private final FolderRepository folderRepository;

    public Request toEntity(ApiRequest apiRequest) {
        Request request = Request.builder()
                .name(apiRequest.name())
                .description(apiRequest.description())
                .method(apiRequest.method())
                .url(apiRequest.url())
                .headers(apiRequest.headers())

                .body(apiRequest.body())
                .preRequestScript(apiRequest.preRequestScript())
                .testsScript(apiRequest.testsScript())
                .build();
        // Set folder if folderId is provided
        if (apiRequest.folderId() != null) {
            folderRepository.findById(apiRequest.folderId())
                    .ifPresent(request::setFolder);
        }
        return request;
    }

    public RequestResponse toResponse(Request request) {
        return RequestResponse.builder()
                .id(request.getId())
                .name(request.getName())
                .description(request.getDescription())
                .method(request.getMethod())
                .url(request.getUrl())
                .headers(request.getHeaders())
                .body(request.getBody())
                .preRequestScript(request.getPreRequestScript())
                .testsScript(request.getTestsScript())
                .collectionId(request.getCollection().getId())
                .collectionName(request.getCollection().getName())
                .folderId(request.getFolder() != null ? request.getFolder().getId() : null)
                .folderName(request.getFolder() != null ? request.getFolder().getName() : null)
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}