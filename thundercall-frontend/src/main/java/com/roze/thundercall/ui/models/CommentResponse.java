package com.roze.thundercall.ui.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** One message in a Postman-style inline comment thread anchored to a
 * request body/script text range. The root of a thread carries the
 * anchor (fieldName/rangeStart/rangeEnd/snippet) and its replies; a
 * reply's own {@code replies} list is always empty. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentResponse {
    private Long id;
    private Long requestId;
    private String fieldName;
    private Integer rangeStart;
    private Integer rangeEnd;
    private String snippet;
    private String message;
    private Long authorId;
    private String authorName;
    private Boolean resolved;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentResponse> replies = new ArrayList<>();

    /** Null-safe accessor — a reply's own {@code replies} often
     * deserializes as null rather than an empty list. */
    public List<CommentResponse> getRepliesOrEmpty() {
        return replies != null ? replies : new ArrayList<>();
    }
}
