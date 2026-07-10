package com.roze.thundercall.ui.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiResponse {
    private int statusCode;
    private String response;
    private String responseHeaders;
    private long duration;
    private boolean success;
}
