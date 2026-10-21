package com.roze.thundercall.ui.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionData {
    @Builder.Default
    private int activeTabIndex = 0;
    @Builder.Default
    private List<TabStateDto> tabs = new ArrayList<>();
    private String selectedEnvironment;
    @Builder.Default
    private Set<String> expandedTreeKeys = new HashSet<>();
    private String selectedTreeKey;
    private Long activeWorkspaceId;
}
