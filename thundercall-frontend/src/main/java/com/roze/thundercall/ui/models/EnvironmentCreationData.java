package com.roze.thundercall.ui.models;

import java.util.Map;

public class EnvironmentCreationData {
    private String name;
    private String description;
    private Map<String, String> variables;

    public EnvironmentCreationData(String name, String description, Map<String, String> variables) {
        this.name = name;
        this.description = description;
        this.variables = variables;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, String> getVariables() {
        return variables;
    }
}