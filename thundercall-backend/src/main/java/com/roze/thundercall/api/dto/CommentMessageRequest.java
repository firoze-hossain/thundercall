package com.roze.thundercall.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Just the text — used both for "Add a reply" and for editing an
 * existing message, since both only ever change the message body. */
public record CommentMessageRequest(@NotBlank String message) {
}
