package com.roze.thundercall.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {
    private int statusCode;
    // Text responses: plain text. Binary responses (PDF, Excel, zip, images):
    // Base64-encoded bytes — the frontend decodes and saves them byte-for-byte.
    private String response;
    private String responseHeaders;
    private long duration;
    private boolean success;
    private boolean binary;
    private String contentType;
    private String fileName;
    private long sizeBytes;
}