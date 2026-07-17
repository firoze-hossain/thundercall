package com.roze.thundercall.ui.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MonitorRequest {
    private String name;
    private Long collectionId;
    private Long environmentId;
    private int intervalMinutes;
    private boolean enabled;
    private boolean notifyOnFailure;
    private String notifyEmail;
}