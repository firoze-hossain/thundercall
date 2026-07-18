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
import com.roze.thundercall.api.security.WorkspaceAccessGuard;
import com.roze.thundercall.api.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** FIX: every write here now goes through WorkspaceAccessGuard — an
 * Editor with shared access to a workspace can create/edit/delete
 * folders in it exactly like the owner can. See CollectionServiceImpl
 * for the same pattern applied there. */
@Service
@RequiredArgsConstructor
public class FolderServiceImpl implements FolderService {
    private final FolderRepository folderRepository;
    private final FolderMapper folderMapper;
    private final CollectionRepository collectionRepository;
    private final WorkspaceAccessGuard workspaceAccessGuard;

    @Override
    @Transactional
    public FolderResponse createFolder(FolderRequest request, User user) {
        Collection collection = findCollectionWithAccess(request.collectionId(), user, true);

        // Nested folders: attach to the parent when one is given
        Folder parent = null;
        if (request.parentFolderId() != null) {
            parent = folderRepository.findById(request.parentFolderId())
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
                ? folderRepository.findByNameAndCollectionIdAndParentFolderId(
                request.name(), request.collectionId(), parent.getId())
                : folderRepository.findByNameAndCollectionIdAndParentFolderIsNull(
                request.name(), request.collectionId());
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
        findCollectionWithAccess(collectionId, user, false); // access check
        return folderRepository.findByCollectionId(collectionId).stream()
                .map(folderMapper::toResponse)
                .toList();
    }

    @Override
    public FolderResponse getFolderById(Long id, User user) {
        Folder folder = findFolderWithAccess(id, user, false);
        return folderMapper.toResponse(folder);
    }

    @Override
    @Transactional
    public FolderResponse updateFolder(Long id, FolderRequest request, User user) {
        Folder folder = findFolderWithAccess(id, user, true);

        // Check if another folder with the same name exists in the same collection
        folderRepository.findByNameAndCollectionId(request.name(), request.collectionId())
                .ifPresent(existingFolder -> {
                    if (!existingFolder.getId().equals(id)) {
                        throw new IllegalArgumentException("Folder with name '" + request.name() + "' already exists in this collection");
                    }
                });

        // If collection changed, verify new collection exists and user has access
        if (!folder.getCollection().getId().equals(request.collectionId())) {
            Collection newCollection = findCollectionWithAccess(request.collectionId(), user, true);
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
        Folder folder = findFolderWithAccess(id, user, true);

        // Move all requests in this folder to the collection root
        if (folder.getRequests() != null && !folder.getRequests().isEmpty()) {
            folder.getRequests().forEach(request -> request.setFolder(null));
        }

        folderRepository.delete(folder);
    }

    private Collection findCollectionWithAccess(Long collectionId, User user, boolean requireWrite) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found"));
        if (requireWrite) {
            workspaceAccessGuard.requireWrite(collection.getWorkspace(), user);
        } else {
            workspaceAccessGuard.requireRead(collection.getWorkspace(), user);
        }
        return collection;
    }

    private Folder findFolderWithAccess(Long id, User user, boolean requireWrite) {
        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
        if (requireWrite) {
            workspaceAccessGuard.requireWrite(folder.getCollection().getWorkspace(), user);
        } else {
            workspaceAccessGuard.requireRead(folder.getCollection().getWorkspace(), user);
        }
        return folder;
    }
}