package com.roze.thundercall.ui.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MockCallLogResponse {
    private Long id;
    private Long mockServerId;
    private String timestamp;
    private String method;
    private String path;
    private String queryParams;
    private String requestHeaders;
    private String requestBody;
    private int responseStatus;
    private String responseHeaders;
    private String responseBody;
    private long durationMs;
    private boolean matched;
    private String matchedRoutePattern;
}
