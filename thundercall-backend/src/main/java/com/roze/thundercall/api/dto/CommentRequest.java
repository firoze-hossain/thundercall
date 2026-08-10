package com.roze.thundercall.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Starts a new comment thread anchored to a text selection — Postman's
 * right-click "Comment" on selected body/script text. */
public record CommentRequest(
        @NotBlank String message,
        @NotNull Integer rangeStart,
        @NotNull Integer rangeEnd,
        String snippet,
        String fieldName
) {
}
