package com.roze.thundercall.service;

import com.roze.thundercall.dto.CollectionRequest;
import com.roze.thundercall.dto.CollectionResponse;
import com.roze.thundercall.entity.User;

import java.util.List;

public interface CollectionService {
    CollectionResponse createCollection(CollectionRequest request, User user);

    List<CollectionResponse> getUserCollections(User user);

    CollectionResponse getCollectionById(Long id, User user);

    CollectionResponse getCollectionWithDetails(Long id, User user);

    CollectionResponse updateCollection(Long id, CollectionRequest request, User user);

    void deleteCollection(Long id, User user);
}
