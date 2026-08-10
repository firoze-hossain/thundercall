package com.roze.thundercall.api.service;

import com.roze.thundercall.api.dto.CommentMessageRequest;
import com.roze.thundercall.api.dto.CommentRequest;
import com.roze.thundercall.api.dto.CommentResponse;
import com.roze.thundercall.api.entity.User;

import java.util.List;

public interface RequestCommentService {
    CommentResponse createThread(Long requestId, CommentRequest request, User user);

    List<CommentResponse> getThreadsForRequest(Long requestId, User user);

    CommentResponse addReply(Long commentId, CommentMessageRequest request, User user);

    CommentResponse editMessage(Long commentId, CommentMessageRequest request, User user);

    void deleteMessage(Long commentId, User user);

    CommentResponse setResolved(Long commentId, boolean resolved, User user);
}
