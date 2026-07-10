package com.roze.thundercall.api.service.impl;

import com.roze.thundercall.api.dto.CollectionRequest;
import com.roze.thundercall.api.dto.CollectionResponse;
import com.roze.thundercall.api.dto.FolderResponse;
import com.roze.thundercall.api.dto.RequestResponse;
import com.roze.thundercall.api.entity.Collection;
import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.entity.Workspace;
import com.roze.thundercall.api.exception.ResourceNotFoundException;
import com.roze.thundercall.api.mapper.CollectionMapper;
import com.roze.thundercall.api.mapper.FolderMapper;
import com.roze.thundercall.api.mapper.RequestMapper;
import com.roze.thundercall.api.repository.CollectionRepository;
import com.roze.thundercall.api.repository.FolderRepository;
import com.roze.thundercall.api.repository.RequestRepository;
import com.roze.thundercall.api.service.CollectionService;
import com.roze.thundercall.api.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * FIX: createCollection() no longer throws "No workspace found".
 * It asks WorkspaceService to get-or-create the user's default workspace,
 * which also heals accounts registered before workspaces were auto-created.
 */
@Service
@RequiredArgsConstructor
public class CollectionServiceImpl implements CollectionService {
    private final CollectionRepository collectionRepository;
    private final CollectionMapper collectionMapper;
    private final WorkspaceService workspaceService;
    private final FolderRepository folderRepository;
    private final FolderMapper folderMapper;
    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;

    @Override
    @Transactional
    public CollectionResponse createCollection(CollectionRequest request, User user) {
        Workspace workspace = workspaceService.getOrCreateDefaultWorkspace(user);
        Collection collection = collectionMapper.toEntity(request);
        collection.setWorkspace(workspace);
        collection.setRequests(new ArrayList<>());
        Collection savedCollection = collectionRepository.save(collection);
        return collectionMapper.toShortResponse(savedCollection);
    }

    @Override
    public List<CollectionResponse> getUserCollections(User user) {
        List<Collection> collections = collectionRepository.findByWorkspaceOwner(user);
        return collections.stream().map(collectionMapper::toShortResponse).toList();
    }

    @Override
    public CollectionResponse getCollectionWithDetails(Long id, User user) {
        Collection collection = collectionRepository.findByIdAndWorkspaceOwner(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found"));

        List<FolderResponse> folderResponses = folderRepository
                .findByCollectionIdAndCollectionWorkspaceOwner(id, user)
                .stream()
                .map(folder -> {
                    FolderResponse folderResponse = folderMapper.toResponse(folder);
                    Long requestCount = folderRepository.countRequestsByFolderId(folder.getId());
                    folderResponse.setRequestCount(requestCount != null ? requestCount.intValue() : 0);
                    return folderResponse;
                })
                .toList();

        List<RequestResponse> requestResponses = requestRepository.findByCollectionId(id)
                .stream()
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
                        .build())
                .toList();
        return collectionMapper.toDetailedResponse(collection, folderResponses, requestResponses);
    }

    @Override
    public CollectionResponse getCollectionById(Long id, User user) {
        Collection collection = collectionRepository.findByIdAndWorkspaceOwner(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found"));
        return collectionMapper.toResponse(collection);
    }

    @Override
    @Transactional
    public CollectionResponse updateCollection(Long id, CollectionRequest request, User user) {
        Collection collection = collectionRepository.findByIdAndWorkspaceOwner(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found"));
        collection.setName(request.name());
        collection.setDescription(request.description());
        Collection updatedCollection = collectionRepository.save(collection);
        return collectionMapper.toShortResponse(updatedCollection);
    }

    @Override
    @Transactional
    public void deleteCollection(Long id, User user) {
        Collection collection = collectionRepository.findByIdAndWorkspaceOwner(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found"));
        collectionRepository.delete(collection);
    }
}