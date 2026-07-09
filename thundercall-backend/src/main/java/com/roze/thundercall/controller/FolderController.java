package com.roze.thundercall.controller;

import com.roze.thundercall.dto.FolderRequest;
import com.roze.thundercall.dto.FolderResponse;
import com.roze.thundercall.entity.User;
import com.roze.thundercall.service.AuthService;
import com.roze.thundercall.service.FolderService;
import com.roze.thundercall.utils.BaseController;
import com.roze.thundercall.utils.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/folders")
@RequiredArgsConstructor
public class FolderController extends BaseController {
    private final FolderService folderService;
    private final AuthService authService;

    @PostMapping("")
    public ResponseEntity<BaseResponse<FolderResponse>> createFolder(
            @Valid @RequestBody FolderRequest request, 
            Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        FolderResponse response = folderService.createFolder(request, user);
        return created(response, "Folder created successfully");
    }

    @GetMapping("")
    public ResponseEntity<BaseResponse<List<FolderResponse>>> getUserFolders(Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        List<FolderResponse> responses = folderService.getUserFolders(user);
        return ok(responses);
    }

    @GetMapping("/collection/{collectionId}")
    public ResponseEntity<BaseResponse<List<FolderResponse>>> getCollectionFolders(
            @PathVariable Long collectionId, 
            Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        List<FolderResponse> responses = folderService.getCollectionFolders(collectionId, user);
        return ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<FolderResponse>> getFolder(
            @PathVariable Long id, 
            Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        FolderResponse response = folderService.getFolderById(id, user);
        return ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<FolderResponse>> updateFolder(
            @PathVariable Long id, 
            @Valid @RequestBody FolderRequest request, 
            Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        FolderResponse response = folderService.updateFolder(id, request, user);
        return ok(response, "Folder updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> deleteFolder(
            @PathVariable Long id, 
            Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        folderService.deleteFolder(id, user);
        return noContent("Folder deleted successfully");
    }
}