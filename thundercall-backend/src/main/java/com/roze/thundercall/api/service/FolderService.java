package com.roze.thundercall.api.service;

import com.roze.thundercall.api.dto.FolderRequest;
import com.roze.thundercall.api.dto.FolderResponse;
import com.roze.thundercall.api.entity.User;

import java.util.List;

public interface FolderService {
    FolderResponse createFolder(FolderRequest request, User user);
    
    List<FolderResponse> getUserFolders(User user);
    
    List<FolderResponse> getCollectionFolders(Long collectionId, User user);
    
    FolderResponse getFolderById(Long id, User user);
    
    FolderResponse updateFolder(Long id, FolderRequest request, User user);
    
    void deleteFolder(Long id, User user);
}