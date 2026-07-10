package com.roze.thundercall.ui.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.roze.thundercall.ui.models.BaseResponse;
import com.roze.thundercall.ui.models.FolderRequest;
import com.roze.thundercall.ui.models.FolderResponse;
import com.roze.thundercall.ui.utils.AlertUtils;
import javafx.application.Platform;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class FolderService {
    private static final String BASE_URL = "/folders";

    public static Optional<FolderResponse> createFolder(String name, String description, Long collectionId) {
        try {
            FolderRequest request = new FolderRequest(name, description, collectionId);
            BaseResponse<FolderResponse> response = ApiClient.post(BASE_URL, request, new TypeReference<BaseResponse<FolderResponse>>() {
            });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to create folder: " + e.getMessage()));
        }
        return Optional.empty();
    }

    public static Optional<List<FolderResponse>> getUserFolders() {
        try {
            BaseResponse<List<FolderResponse>> response = ApiClient.get(BASE_URL, new TypeReference<BaseResponse<List<FolderResponse>>>() {
            });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to get folders: " + e.getMessage()));
        }
        return Optional.empty();
    }

    public static Optional<List<FolderResponse>> getCollectionFolders(Long collectionId) {
        try {
            BaseResponse<List<FolderResponse>> response = ApiClient.get(BASE_URL + "/collection/" + collectionId, new TypeReference<BaseResponse<List<FolderResponse>>>() {
            });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to get collection folders: " + e.getMessage()));
        }
        return Optional.empty();
    }

    public static Optional<FolderResponse> getFolderById(Long id) {
        try {
            BaseResponse<FolderResponse> response = ApiClient.get(BASE_URL + "/" + id, new TypeReference<BaseResponse<FolderResponse>>() {
            });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to get folder: " + e.getMessage()));
        }
        return Optional.empty();
    }

    public static Optional<FolderResponse> updateFolder(Long id, String name, String description, Long collectionId) {
        try {
            FolderRequest request = new FolderRequest(name, description, collectionId);
            BaseResponse<FolderResponse> response = ApiClient.put(BASE_URL + "/" + id, request, new TypeReference<BaseResponse<FolderResponse>>() {
            });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to update folder: " + e.getMessage()));
        }
        return Optional.empty();
    }

    public static boolean deleteFolder(Long id) {
        try {
            BaseResponse<Void> response = ApiClient.delete(BASE_URL + "/" + id, new TypeReference<BaseResponse<Void>>() {
            });
            return response != null && response.isSuccess();
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to delete folder: " + e.getMessage()));
            return false;
        }
    }


}