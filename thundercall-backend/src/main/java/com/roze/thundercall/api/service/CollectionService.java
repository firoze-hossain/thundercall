package com.roze.thundercall.api.service;

import com.roze.thundercall.api.dto.CollectionRequest;
import com.roze.thundercall.api.dto.CollectionResponse;
import com.roze.thundercall.api.entity.User;

import java.util.List;
import java.util.Map;

public interface CollectionService {
    CollectionResponse createCollection(CollectionRequest request, User user);

    List<CollectionResponse> getUserCollections(User user);

    CollectionResponse getCollectionById(Long id, User user);

    CollectionResponse getCollectionWithDetails(Long id, User user);

    CollectionResponse updateCollection(Long id, CollectionRequest request, User user);

    void deleteCollection(Long id, User user);

    /** Replaces this collection's variables outright (Postman's "Set as
     * variable" > Collection scope writes here). */
    CollectionResponse updateCollectionVariables(Long id, Map<String, String> variables, User user);
}
