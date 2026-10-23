package com.roze.thundercall.ui.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestHistoryResponse {
    private Long id;
    private Long requestId;
    private String requestName;
    private String url;
    private String method;
    private String requestHeaders;
    private String requestBody;
    private LocalDateTime timestamp;
    private Integer statusCode;
    private Long duration;
    private String response;
    private String responseHeaders;
    private Boolean success;
}