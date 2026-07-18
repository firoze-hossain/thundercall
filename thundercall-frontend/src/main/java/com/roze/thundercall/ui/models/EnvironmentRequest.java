package com.roze.thundercall.ui.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnvironmentRequest {
    private String name;
    private String description;
    private Map<String, String> variables;

    @JsonProperty("isActive")
    private Boolean isActive;

    // Optional — which workspace to create this in. Null falls back to
    // the default workspace, same as before this field existed.
    private Long workspaceId;

    public EnvironmentRequest(String name, String description, Map<String, String> variables, Boolean isActive) {
        this.name = name;
        this.description = description;
        this.variables = variables;
        this.isActive = isActive != null ? isActive : true;
    }
}