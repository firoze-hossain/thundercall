package com.roze.thundercall.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.roze.thundercall.models.BaseResponse;
import com.roze.thundercall.models.CollectionRequest;
import com.roze.thundercall.models.CollectionResponse;
import com.roze.thundercall.utils.AlertUtils;
import javafx.application.Platform;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class CollectionService {
    private static final String BASE_URL = "/collections";

    public static Optional<CollectionResponse> createCollection(CollectionRequest request) {
        try {
            BaseResponse<CollectionResponse> response = ApiClient.post(BASE_URL, request, new TypeReference<BaseResponse<CollectionResponse>>() {
            });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to create collection: " + e.getMessage()));
        }
        return Optional.empty();
    }

    public static Optional<List<CollectionResponse>> getUserCollections() {
        try {
            BaseResponse<List<CollectionResponse>> response = ApiClient.get(BASE_URL, new TypeReference<BaseResponse<List<CollectionResponse>>>() {
            });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to get collections: " + e.getMessage()));
        }
        return Optional.empty();
    }

    public static Optional<CollectionResponse> getCollectionById(Long id) {
        try {
            BaseResponse<CollectionResponse> response = ApiClient.get(BASE_URL + "/" + id, new TypeReference<BaseResponse<CollectionResponse>>() {
            });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to get Collection: " + e.getMessage()));
        }
        return Optional.empty();
    }

    public static Optional<CollectionResponse> getCollectionWithDetails(Long id) {
        try {
            BaseResponse<CollectionResponse> response = ApiClient.get(BASE_URL + "/" + id + "/details", new TypeReference<BaseResponse<CollectionResponse>>() {
            });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to get collection details: " + e.getMessage()));
        }
        return Optional.empty();
    }

    public static Optional<CollectionResponse> updateCollection(Long id, CollectionRequest request) {
        try {
            BaseResponse<CollectionResponse> response = ApiClient.put(BASE_URL + "/" + id, request, new TypeReference<BaseResponse<CollectionResponse>>() {
            });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to update collection: " + e.getMessage()));
        }
        return Optional.empty();
    }

    public static boolean deleteCollection(Long id) {
        try {
            BaseResponse<Void> response = ApiClient.delete(BASE_URL + "/" + id, new TypeReference<BaseResponse<Void>>() {
            });
            return response != null && response.isSuccess();
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to delete collection: " + e.getMessage()));
            return false;
        }
    }
}
