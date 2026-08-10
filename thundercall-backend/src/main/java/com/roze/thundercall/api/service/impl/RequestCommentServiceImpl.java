package com.roze.thundercall.api.service.impl;

import com.roze.thundercall.api.dto.CommentMessageRequest;
import com.roze.thundercall.api.dto.CommentRequest;
import com.roze.thundercall.api.dto.CommentResponse;
import com.roze.thundercall.api.entity.Request;
import com.roze.thundercall.api.entity.RequestComment;
import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.exception.AuthException;
import com.roze.thundercall.api.exception.ResourceNotFoundException;
import com.roze.thundercall.api.mapper.RequestCommentMapper;
import com.roze.thundercall.api.repository.RequestCommentRepository;
import com.roze.thundercall.api.repository.RequestRepository;
import com.roze.thundercall.api.security.WorkspaceAccessGuard;
import com.roze.thundercall.api.service.RequestCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Postman-style inline body/script comments. Creating, reading and
 * replying only require read access to the request's workspace (the same
 * rule as viewing the request itself) — commenting doesn't change the
 * request, so a Viewer can leave feedback too. Editing or deleting a
 * specific message additionally requires being that message's own
 * author; there's no moderator override, matching how Postman only lets
 * you edit or delete your own comments.
 */
@Service
@RequiredArgsConstructor
public class RequestCommentServiceImpl implements RequestCommentService {
    private final RequestCommentRepository requestCommentRepository;
    private final RequestRepository requestRepository;
    private final RequestCommentMapper requestCommentMapper;
    private final WorkspaceAccessGuard workspaceAccessGuard;

    @Override
    @Transactional
    public CommentResponse createThread(Long requestId, CommentRequest request, User user) {
        Request req = findRequestWithAccess(requestId, user);
        RequestComment comment = RequestComment.builder()
                .request(req)
                .author(user)
                .fieldName(request.fieldName() != null ? request.fieldName() : "body")
                .rangeStart(request.rangeStart())
                .rangeEnd(request.rangeEnd())
                .snippet(request.snippet())
                .message(request.message())
                .resolved(false)
                .build();
        RequestComment saved = requestCommentRepository.save(comment);
        return requestCommentMapper.toResponse(saved);
    }

    @Override
    public List<CommentResponse> getThreadsForRequest(Long requestId, User user) {
        Request req = findRequestWithAccess(requestId, user);
        return requestCommentRepository.findByRequestIdAndParentIsNullOrderByCreatedAtAsc(req.getId())
                .stream()
                .map(requestCommentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CommentResponse addReply(Long commentId, CommentMessageRequest request, User user) {
        RequestComment root = findCommentWithAccess(commentId, user);
        RequestComment reply = RequestComment.builder()
                .request(root.getRequest())
                .parent(root)
                .author(user)
                .fieldName(root.getFieldName())
                .message(request.message())
                .resolved(false)
                .build();
        requestCommentRepository.save(reply);
        // Return the whole (now-updated) thread rather than just the new
        // reply, so the UI can simply redraw the popup from the response.
        return requestCommentMapper.toResponse(reloadRoot(root.getId()));
    }

    @Override
    @Transactional
    public CommentResponse editMessage(Long commentId, CommentMessageRequest request, User user) {
        RequestComment comment = findCommentWithAccess(commentId, user);
        requireAuthor(comment, user);
        comment.setMessage(request.message());
        RequestComment updated = requestCommentRepository.save(comment);
        Long rootId = updated.getParent() != null ? updated.getParent().getId() : updated.getId();
        return requestCommentMapper.toResponse(reloadRoot(rootId));
    }

    @Override
    @Transactional
    public void deleteMessage(Long commentId, User user) {
        RequestComment comment = findCommentWithAccess(commentId, user);
        requireAuthor(comment, user);
        // Deleting the root deletes the whole thread (cascade, see the
        // entity's replies mapping); deleting a reply just removes that
        // one message.
        requestCommentRepository.delete(comment);
    }

    @Override
    @Transactional
    public CommentResponse setResolved(Long commentId, boolean resolved, User user) {
        RequestComment comment = findCommentWithAccess(commentId, user);
        RequestComment root = comment.getParent() != null ? comment.getParent() : comment;
        root.setResolved(resolved);
        RequestComment updated = requestCommentRepository.save(root);
        return requestCommentMapper.toResponse(updated);
    }

    private RequestComment reloadRoot(Long rootId) {
        return requestCommentRepository.findById(rootId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
    }

    private void requireAuthor(RequestComment comment, User user) {
        if (!comment.getAuthor().getId().equals(user.getId())) {
            throw new AuthException("You can only edit or delete your own comments");
        }
    }

    private Request findRequestWithAccess(Long requestId, User user) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        workspaceAccessGuard.requireRead(request.getCollection().getWorkspace(), user);
        return request;
    }

    private RequestComment findCommentWithAccess(Long commentId, User user) {
        RequestComment comment = requestCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        findRequestWithAccess(comment.getRequest().getId(), user);
        return comment;
    }
}
