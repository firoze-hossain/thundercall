package com.roze.thundercall.service.impl;

import com.roze.thundercall.dto.EnvironmentRequest;
import com.roze.thundercall.dto.EnvironmentResponse;
import com.roze.thundercall.entity.Environment;
import com.roze.thundercall.entity.User;
import com.roze.thundercall.entity.Workspace;
import com.roze.thundercall.exception.ResourceNotFoundException;
import com.roze.thundercall.mapper.EnvironmentMapper;
import com.roze.thundercall.repository.EnvironmentRepository;
import com.roze.thundercall.repository.WorkspaceRepository;
import com.roze.thundercall.service.EnvironmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnvironmentServiceImpl implements EnvironmentService {
    private final EnvironmentRepository environmentRepository;
    private final EnvironmentMapper environmentMapper;
    private final WorkspaceRepository workspaceRepository;

    @Override
    @Transactional
    public EnvironmentResponse createEnvironment(EnvironmentRequest request, User user) {
        Workspace workspace = workspaceRepository.findByOwner(user)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No workspace found"));
        
        // Check if environment with same name already exists
        environmentRepository.findByNameAndWorkspaceOwner(request.name(), user)
                .ifPresent(env -> {
                    throw new IllegalArgumentException("Environment with name '" + request.name() + "' already exists");
                });
        
        Environment environment = environmentMapper.toEntity(request);
        environment.setWorkspace(workspace);
        
        Environment savedEnvironment = environmentRepository.save(environment);
        return environmentMapper.toResponse(savedEnvironment);
    }

    @Override
    public List<EnvironmentResponse> getUserEnvironments(User user) {
        List<Environment> environments = environmentRepository.findByWorkspaceOwner(user);
        return environments.stream()
                .map(environmentMapper::toResponse)
                .toList();
    }

    @Override
    public EnvironmentResponse getEnvironmentById(Long id, User user) {
        Environment environment = environmentRepository.findByIdAndWorkspaceOwner(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Environment not found"));
        return environmentMapper.toResponse(environment);
    }

    @Override
    @Transactional
    public EnvironmentResponse updateEnvironment(Long id, EnvironmentRequest request, User user) {
        Environment environment = environmentRepository.findByIdAndWorkspaceOwner(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Environment not found"));
        
        // Check if another environment with the same name exists (excluding current one)
        environmentRepository.findByNameAndWorkspaceOwner(request.name(), user)
                .ifPresent(existingEnv -> {
                    if (!existingEnv.getId().equals(id)) {
                        throw new IllegalArgumentException("Environment with name '" + request.name() + "' already exists");
                    }
                });
        
        environment.setName(request.name());
        environment.setDescription(request.description());
        environment.setVariables(request.variables());
        if (request.isActive() != null) {
            environment.setIsActive(request.isActive());
        }
        
        Environment updatedEnvironment = environmentRepository.save(environment);
        return environmentMapper.toResponse(updatedEnvironment);
    }

    @Override
    @Transactional
    public void deleteEnvironment(Long id, User user) {
        Environment environment = environmentRepository.findByIdAndWorkspaceOwner(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Environment not found"));
        environmentRepository.delete(environment);
    }

    @Override
    public List<EnvironmentResponse> getActiveEnvironments(User user) {
        List<Environment> environments = environmentRepository.findByWorkspaceOwnerAndIsActive(user, true);
        return environments.stream()
                .map(environmentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public EnvironmentResponse toggleEnvironmentStatus(Long id, User user, Boolean isActive) {
        Environment environment = environmentRepository.findByIdAndWorkspaceOwner(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Environment not found"));
        
        environment.setIsActive(isActive);
        Environment updatedEnvironment = environmentRepository.save(environment);
        return environmentMapper.toResponse(updatedEnvironment);
    }
}