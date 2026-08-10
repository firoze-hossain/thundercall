package com.roze.thundercall.ui.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Starts a new comment thread anchored to a text selection — mirrors the
 * backend's CommentRequest record. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentRequest {
    private String message;
    private Integer rangeStart;
    private Integer rangeEnd;
    private String snippet;
    private String fieldName;
}
