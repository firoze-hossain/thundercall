package com.roze.thundercall.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
//@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnvironmentRequest {
    private String name;
    private String description;
    private Map<String, String> variables;
    
    @JsonProperty("isActive")
    private Boolean isActive;

    public EnvironmentRequest(String name, String description, Map<String, String> variables, Boolean isActive) {
        this.name = name;
        this.description = description;
        this.variables = variables;
        this.isActive = isActive != null ? isActive : true;
    }
}