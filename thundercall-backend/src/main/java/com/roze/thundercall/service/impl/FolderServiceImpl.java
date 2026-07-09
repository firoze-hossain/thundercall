package com.roze.thundercall.service.impl;

import com.roze.thundercall.dto.FolderRequest;
import com.roze.thundercall.dto.FolderResponse;
import com.roze.thundercall.entity.Collection;
import com.roze.thundercall.entity.Folder;
import com.roze.thundercall.entity.User;
import com.roze.thundercall.exception.ResourceNotFoundException;
import com.roze.thundercall.mapper.FolderMapper;
import com.roze.thundercall.repository.CollectionRepository;
import com.roze.thundercall.repository.FolderRepository;
import com.roze.thundercall.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        
        // Check if folder with same name already exists in this collection
        folderRepository.findByNameAndCollectionIdAndCollectionWorkspaceOwner(
                request.name(), request.collectionId(), user)
                .ifPresent(folder -> {
                    throw new IllegalArgumentException("Folder with name '" + request.name() + "' already exists in this collection");
                });
        
        Folder folder = folderMapper.toEntity(request);
        folder.setCollection(collection);
        
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