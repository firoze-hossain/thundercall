package com.roze.thundercall.api.controller;

import com.roze.thundercall.api.dto.CommentMessageRequest;
import com.roze.thundercall.api.dto.CommentRequest;
import com.roze.thundercall.api.dto.CommentResponse;
import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.service.AuthService;
import com.roze.thundercall.api.service.RequestCommentService;
import com.roze.thundercall.api.utils.BaseController;
import com.roze.thundercall.api.utils.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Postman-style inline comments on a request's body/scripts — right-click
 * "Comment" on selected text opens a thread; replies, edits, deletes and
 * resolve all live here. See RequestCommentServiceImpl for access rules.
 */
@RestController
@RequiredArgsConstructor
public class RequestCommentController extends BaseController {
    private final RequestCommentService requestCommentService;
    private final AuthService authService;

    @PostMapping("/requests/{requestId}/comments")
    public ResponseEntity<BaseResponse<CommentResponse>> createThread(
            @PathVariable Long requestId,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        CommentResponse response = requestCommentService.createThread(requestId, request, user);
        return created(response, "Comment added");
    }

    @GetMapping("/requests/{requestId}/comments")
    public ResponseEntity<BaseResponse<List<CommentResponse>>> getThreads(
            @PathVariable Long requestId,
            Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        List<CommentResponse> responses = requestCommentService.getThreadsForRequest(requestId, user);
        return ok(responses);
    }

    @PostMapping("/comments/{commentId}/replies")
    public ResponseEntity<BaseResponse<CommentResponse>> addReply(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentMessageRequest request,
            Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        CommentResponse response = requestCommentService.addReply(commentId, request, user);
        return ok(response, "Reply added");
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<BaseResponse<CommentResponse>> editMessage(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentMessageRequest request,
            Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        CommentResponse response = requestCommentService.editMessage(commentId, request, user);
        return ok(response, "Comment updated");
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<BaseResponse<Void>> deleteMessage(
            @PathVariable Long commentId,
            Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        requestCommentService.deleteMessage(commentId, user);
        return noContent("Comment deleted");
    }

    @PatchMapping("/comments/{commentId}/resolve")
    public ResponseEntity<BaseResponse<CommentResponse>> setResolved(
            @PathVariable Long commentId,
            @RequestParam boolean resolved,
            Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        CommentResponse response = requestCommentService.setResolved(commentId, resolved, user);
        return ok(response, resolved ? "Marked resolved" : "Reopened");
    }
}
