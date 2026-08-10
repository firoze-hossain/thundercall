package com.roze.thundercall.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    /** Empty/omitted on a reply — only the root comment of a thread
     * carries its replies. */
    private List<CommentResponse> replies;
}
