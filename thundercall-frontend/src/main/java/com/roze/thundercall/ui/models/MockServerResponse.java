package com.roze.thundercall.ui.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MockServerResponse {
    private Long id;
    private String name;
    private String description;
    private boolean enabled;
    private String baseUrl;
    private long routeCount;
    private String createdAt;
}