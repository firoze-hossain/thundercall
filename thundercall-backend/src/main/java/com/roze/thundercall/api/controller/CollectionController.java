package com.roze.thundercall.api.controller;

import com.roze.thundercall.api.dto.CollectionRequest;
import com.roze.thundercall.api.dto.CollectionResponse;
import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.service.AuthService;
import com.roze.thundercall.api.service.CollectionService;
import com.roze.thundercall.api.utils.BaseController;
import com.roze.thundercall.api.utils.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/collections")
@RequiredArgsConstructor
public class CollectionController extends BaseController {
    private final CollectionService collectionService;
    private final AuthService authService;

    @PostMapping("")
    public ResponseEntity<BaseResponse<CollectionResponse>> createCollection(@Valid @RequestBody CollectionRequest request, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        CollectionResponse response = collectionService.createCollection(request, user);
        return created(response, "Collection created successfully");
    }

    @GetMapping("")
    public ResponseEntity<BaseResponse<List<CollectionResponse>>> getUserCollections(Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        List<CollectionResponse> responses = collectionService.getUserCollections(user);
        return ok(responses);
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<BaseResponse<CollectionResponse>> getCollectionWithDetails(
            @PathVariable Long id,
            Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        CollectionResponse response = collectionService.getCollectionWithDetails(id, user);
        return ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<CollectionResponse>> getCollection(@PathVariable Long id, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        CollectionResponse response = collectionService.getCollectionById(id, user);
        return ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<CollectionResponse>> updateCollection(@PathVariable Long id, @Valid @RequestBody CollectionRequest request, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        CollectionResponse response = collectionService.updateCollection(id, request, user);
        return ok(response, "Collection updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> deleteCollection(@PathVariable Long id, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        collectionService.deleteCollection(id, user);
        return noContent("Collection deleted successfully");
    }
}
