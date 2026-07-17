package com.roze.thundercall.ui.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MockRouteRequest {
    // Kept as a plain string (not the frontend HttpMethod enum) since WS
    // makes no sense here and this only ever needs to round-trip through
    // Jackson, never compared against the enum's other logic.
    private String method;
    private String path;
    private Integer responseStatus;
    private String responseBody;
    private String responseHeaders;
    private Integer delayMs;
}