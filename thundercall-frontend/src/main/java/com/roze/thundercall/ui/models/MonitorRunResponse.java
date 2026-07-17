package com.roze.thundercall.ui.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MonitorRunResponse {
    private Long id;
    private String startedAt;
    private String completedAt;
    private int totalRequests;
    private int passedRequests;
    private int failedRequests;
    private long avgResponseTimeMs;
    private boolean success;
    private String details;
}