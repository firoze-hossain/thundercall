package com.roze.thundercall.ui.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MonitorResponse {
    private Long id;
    private String name;
    private Long collectionId;
    private String collectionName;
    private Long environmentId;
    private String environmentName;
    private int intervalMinutes;
    private boolean enabled;
    private boolean notifyOnFailure;
    private String notifyEmail;
    private String lastRunAt;
    private String lastRunStatus;
    private String createdAt;
}