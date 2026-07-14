package com.roze.thundercall.api.service.impl;

import com.roze.thundercall.api.dto.FolderRequest;
import com.roze.thundercall.api.dto.FolderResponse;
import com.roze.thundercall.api.entity.Collection;
import com.roze.thundercall.api.entity.Folder;
import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.exception.ResourceNotFoundException;
import com.roze.thundercall.api.mapper.FolderMapper;
import com.roze.thundercall.api.repository.CollectionRepository;
import com.roze.thundercall.api.repository.FolderRepository;
import com.roze.thundercall.api.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FolderServiceImpl implements FolderService {
    private final FolderRepository folderRepository;
    private final FolderMapper folderMapper;
    private final CollectionRepository collectionRepository;

    @Override
    @Transactional
    public FolderResponse createFolder(FolderRequest request, User user) {
        Collection collection = collectionRepository.findByIdAndWorkspaceOwner(request.collectionId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found"));

        // Nested folders: attach to the parent when one is given
        Folder parent = null;
        if (request.parentFolderId() != null) {
            parent = folderRepository
                    .findByIdAndCollectionWorkspaceOwner(request.parentFolderId(), user)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent folder not found"));
            if (!parent.getCollection().getId().equals(collection.getId())) {
                throw new IllegalArgumentException("Parent folder belongs to a different collection");
            }
        }

        // FIX: the duplicate-name check now scopes to the PARENT folder
        // (or "top-level" when there's no parent), not the whole
        // collection — two folders can share a name as long as they live
        // in different places, exactly like a real filesystem. Previously
        // a second, legitimately-nested folder named e.g. "common" under a
        // different parent was wrongly rejected as already existing.
        Optional<Folder> duplicate = parent != null
                ? folderRepository.findByNameAndCollectionIdAndParentFolderIdAndCollectionWorkspaceOwner(
                request.name(), request.collectionId(), parent.getId(), user)
                : folderRepository.findByNameAndCollectionIdAndParentFolderIsNullAndCollectionWorkspaceOwner(
                request.name(), request.collectionId(), user);
        if (duplicate.isPresent()) {
            throw new IllegalArgumentException("Folder with name '" + request.name()
                    + "' already exists " + (parent != null ? "in this folder" : "at the top level of this collection"));
        }

        Folder folder = folderMapper.toEntity(request);
        folder.setCollection(collection);
        if (parent != null) {
            folder.setParentFolder(parent);
        }

        Folder savedFolder = folderRepository.save(folder);
        return folderMapper.toResponse(savedFolder);
    }

    @Override
    public List<FolderResponse> getUserFolders(User user) {
        List<Folder> folders = folderRepository.findByCollectionWorkspaceOwner(user);
        return folders.stream()
                .map(folderMapper::toResponse)
                .toList();
    }

    @Override
    public List<FolderResponse> getCollectionFolders(Long collectionId, User user) {
        List<Folder> folders = folderRepository.findByCollectionIdAndCollectionWorkspaceOwner(collectionId, user);
        return folders.stream()
                .map(folderMapper::toResponse)
                .toList();
    }

    @Override
    public FolderResponse getFolderById(Long id, User user) {
        Folder folder = folderRepository.findByIdAndCollectionWorkspaceOwner(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
        return folderMapper.toResponse(folder);
    }

    @Override
    @Transactional
    public FolderResponse updateFolder(Long id, FolderRequest request, User user) {
        Folder folder = folderRepository.findByIdAndCollectionWorkspaceOwner(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));

        // Check if another folder with the same name exists in the same collection
        folderRepository.findByNameAndCollectionIdAndCollectionWorkspaceOwner(
                        request.name(), request.collectionId(), user)
                .ifPresent(existingFolder -> {
                    if (!existingFolder.getId().equals(id)) {
                        throw new IllegalArgumentException("Folder with name '" + request.name() + "' already exists in this collection");
                    }
                });

        // If collection changed, verify new collection exists and user has access
        if (!folder.getCollection().getId().equals(request.collectionId())) {
            Collection newCollection = collectionRepository.findByIdAndWorkspaceOwner(request.collectionId(), user)
                    .orElseThrow(() -> new ResourceNotFoundException("Collection not found"));
            folder.setCollection(newCollection);
        }

        folder.setName(request.name());
        folder.setDescription(request.description());

        Folder updatedFolder = folderRepository.save(folder);
        return folderMapper.toResponse(updatedFolder);
    }

    @Override
    @Transactional
    public void deleteFolder(Long id, User user) {
        Folder folder = folderRepository.findByIdAndCollectionWorkspaceOwner(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));

        // Move all requests in this folder to the collection root
        if (folder.getRequests() != null && !folder.getRequests().isEmpty()) {
            folder.getRequests().forEach(request -> request.setFolder(null));
        }

        folderRepository.delete(folder);
    }
}