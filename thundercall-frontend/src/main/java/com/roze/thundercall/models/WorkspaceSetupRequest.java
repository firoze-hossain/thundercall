package com.roze.thundercall.models;

import lombok.Data;

import java.util.List;

@Data
public class WorkspaceSetupRequest {
    private String workspaceName;
    private Boolean createSampleData;
    private List<String> preferredFeatures;

    public WorkspaceSetupRequest() {
        this.createSampleData = true;
    }

    public WorkspaceSetupRequest(String workspaceName) {
        this.workspaceName = workspaceName;
        this.createSampleData = true;
    }
}
