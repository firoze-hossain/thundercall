package com.roze.thundercall.service;

import com.roze.thundercall.dto.FolderRequest;
import com.roze.thundercall.dto.FolderResponse;
import com.roze.thundercall.entity.User;

import java.util.List;

public interface FolderService {
    FolderResponse createFolder(FolderRequest request, User user);
    
    List<FolderResponse> getUserFolders(User user);
    
    List<FolderResponse> getCollectionFolders(Long collectionId, User user);
    
    FolderResponse getFolderById(Long id, User user);
    
    FolderResponse updateFolder(Long id, FolderRequest request, User user);
    
    void deleteFolder(Long id, User user);
}