package com.roze.thundercall.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WorkspaceSetupRequest {
    private String workspaceName;
    private Boolean createSampleData = true;
    private List<String> preferredFeature;

}
