package com.roze.thundercall.ui.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MockRouteResponse {
    private Long id;
    private Long mockServerId;
    private String method;
    private String path;
    private int responseStatus;
    private String responseBody;
    private String responseHeaders;
    private int delayMs;
    private String createdAt;
}