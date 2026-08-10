package com.roze.thundercall.api.mapper;

import com.roze.thundercall.api.dto.CommentResponse;
import com.roze.thundercall.api.entity.RequestComment;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class RequestCommentMapper {

    public CommentResponse toResponse(RequestComment comment) {
        CommentResponse.CommentResponseBuilder builder = CommentResponse.builder()
                .id(comment.getId())
                .requestId(comment.getRequest().getId())
                .fieldName(comment.getFieldName())
                .rangeStart(comment.getRangeStart())
                .rangeEnd(comment.getRangeEnd())
                .snippet(comment.getSnippet())
                .message(comment.getMessage())
                .authorId(comment.getAuthor().getId())
                .authorName(displayName(comment))
                .resolved(comment.getResolved())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt());

        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            builder.replies(comment.getReplies().stream()
                    .sorted(Comparator.comparing(RequestComment::getCreatedAt))
                    .map(this::toResponse)
                    .toList());
        }
        return builder.build();
    }

    private String displayName(RequestComment comment) {
        String fullName = comment.getAuthor().getFullName();
        return (fullName != null && !fullName.isBlank()) ? fullName : comment.getAuthor().getUsername();
    }
}
