package com.roze.thundercall.ui.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.roze.thundercall.ui.models.BaseResponse;
import com.roze.thundercall.ui.models.CommentMessageRequest;
import com.roze.thundercall.ui.models.CommentRequest;
import com.roze.thundercall.ui.models.CommentResponse;
import com.roze.thundercall.ui.utils.AlertUtils;
import javafx.application.Platform;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/** Postman-style inline comments on a request's body/scripts — backs the
 * right-click "Comment" menu item. Every call here talks to the real
 * backend (see RequestCommentController), so comments are genuinely
 * shared with teammates who have access to the same workspace, not just
 * stored on this machine. */
public class CommentService {

    public static Optional<CommentResponse> createThread(Long requestId, String message, int rangeStart,
                                                           int rangeEnd, String snippet, String fieldName) {
        try {
            CommentRequest request = new CommentRequest(message, rangeStart, rangeEnd, snippet, fieldName);
            BaseResponse<CommentResponse> response = ApiClient.post(
                    "/requests/" + requestId + "/comments", request,
                    new TypeReference<BaseResponse<CommentResponse>>() {
                    });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to add comment: " + e.getMessage()));
        }
        return Optional.empty();
    }

    public static Optional<List<CommentResponse>> getThreadsForRequest(Long requestId) {
        try {
            BaseResponse<List<CommentResponse>> response = ApiClient.get(
                    "/requests/" + requestId + "/comments",
                    new TypeReference<BaseResponse<List<CommentResponse>>>() {
                    });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to load comments: " + e.getMessage()));
        }
        return Optional.empty();
    }

    public static Optional<CommentResponse> addReply(Long commentId, String message) {
        try {
            BaseResponse<CommentResponse> response = ApiClient.post(
                    "/comments/" + commentId + "/replies", new CommentMessageRequest(message),
                    new TypeReference<BaseResponse<CommentResponse>>() {
                    });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to reply: " + e.getMessage()));
        }
        return Optional.empty();
    }

    public static Optional<CommentResponse> editMessage(Long commentId, String message) {
        try {
            BaseResponse<CommentResponse> response = ApiClient.put(
                    "/comments/" + commentId, new CommentMessageRequest(message),
                    new TypeReference<BaseResponse<CommentResponse>>() {
                    });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to update comment: " + e.getMessage()));
        }
        return Optional.empty();
    }

    public static boolean deleteMessage(Long commentId) {
        try {
            BaseResponse<Void> response = ApiClient.delete(
                    "/comments/" + commentId, new TypeReference<BaseResponse<Void>>() {
                    });
            return response != null && response.isSuccess();
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to delete comment: " + e.getMessage()));
            return false;
        }
    }

    public static Optional<CommentResponse> setResolved(Long commentId, boolean resolved) {
        try {
            BaseResponse<CommentResponse> response = ApiClient.patch(
                    "/comments/" + commentId + "/resolve?resolved=" + resolved, null,
                    new TypeReference<BaseResponse<CommentResponse>>() {
                    });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to update comment: " + e.getMessage()));
        }
        return Optional.empty();
    }
}
