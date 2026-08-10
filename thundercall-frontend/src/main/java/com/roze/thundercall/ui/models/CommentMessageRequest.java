package com.roze.thundercall.ui.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Just the text — used for both "Add a reply" and editing an existing
 * message, mirroring the backend's CommentMessageRequest record. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentMessageRequest {
    private String message;
}
