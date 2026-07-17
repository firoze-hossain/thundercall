package com.roze.thundercall.api.dto;

/**
 * One row of a "form-data" request body. type is "text" or "file" —
 * for "file", fileBase64/fileName carry the actual file content (the
 * frontend reads it from disk and encodes it; nothing is ever read from
 * the filesystem on the backend side).
 */
public record FormDataField(
        String key,
        String type,
        String value,
        String fileName,
        String fileBase64
) {
}