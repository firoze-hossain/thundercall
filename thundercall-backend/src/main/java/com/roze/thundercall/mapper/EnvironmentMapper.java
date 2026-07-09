package com.roze.thundercall.mapper;

import com.roze.thundercall.dto.EnvironmentRequest;
import com.roze.thundercall.dto.EnvironmentResponse;
import com.roze.thundercall.entity.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EnvironmentMapper {

    public Environment toEntity(EnvironmentRequest request) {
        return Environment.builder()
                .name(request.name())
                .description(request.description())
                .variables(request.variables() != null ? Map.copyOf(request.variables()) : Map.of())
                .isActive(request.isActive() != null ? request.isActive() : true)
                .build();
    }

    public EnvironmentResponse toResponse(Environment environment) {
        return EnvironmentResponse.builder()
                .id(environment.getId())
                .name(environment.getName())
                .description(environment.getDescription())
                .variables(environment.getVariables())
                .workspaceId(environment.getWorkspace().getId())
                .workspaceName(environment.getWorkspace().getName())
                .isActive(environment.getIsActive())
                .createdAt(environment.getCreatedAt())
                .updatedAt(environment.getUpdatedAt())
                .build();
    }

    public EnvironmentResponse toShortResponse(Environment environment) {
        return EnvironmentResponse.builder()
                .id(environment.getId())
                .name(environment.getName())
                .description(environment.getDescription())
                .variables(environment.getVariables())
                .workspaceId(environment.getWorkspace().getId())
                .workspaceName(environment.getWorkspace().getName())
                .isActive(environment.getIsActive())
                .createdAt(environment.getCreatedAt())
                .updatedAt(environment.getUpdatedAt())
                .build();
    }
}
