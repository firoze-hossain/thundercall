package com.roze.thundercall.api.service.impl;

import com.roze.thundercall.api.dto.EnvironmentRequest;
import com.roze.thundercall.api.dto.EnvironmentResponse;
import com.roze.thundercall.api.entity.Environment;
import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.entity.Workspace;
import com.roze.thundercall.api.exception.ResourceNotFoundException;
import com.roze.thundercall.api.mapper.EnvironmentMapper;
import com.roze.thundercall.api.repository.EnvironmentRepository;
import com.roze.thundercall.api.security.WorkspaceAccessGuard;
import com.roze.thundercall.api.service.EnvironmentService;
import com.roze.thundercall.api.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * FIX: createEnvironment() no longer throws "No workspace found".
 * The user's default workspace is created on demand via WorkspaceService.
 *
 * FIX: every write here now goes through WorkspaceAccessGuard, and
 * createEnvironment() now accepts an explicit workspaceId — an Editor
 * with shared access to a workspace can create/edit/delete
 * environments in it exactly like the owner can. See
 * CollectionServiceImpl for the same pattern applied there.
 */
@Service
@RequiredArgsConstructor
public class EnvironmentServiceImpl implements EnvironmentService {
    private final EnvironmentRepository environmentRepository;
    private final EnvironmentMapper environmentMapper;
    private final WorkspaceService workspaceService;
    private final WorkspaceAccessGuard workspaceAccessGuard;

    @Override
    @Transactional
    public EnvironmentResponse createEnvironment(EnvironmentRequest request, User user) {
        Workspace workspace = request.workspaceId() != null
                ? workspaceAccessGuard.resolveForWrite(request.workspaceId(), user)
                : workspaceService.getOrCreateDefaultWorkspace(user);

        environmentRepository.findByNameAndWorkspaceId(request.name(), workspace.getId())
                .ifPresent(env -> {
                    throw new IllegalArgumentException(
                            "Environment with name '" + request.name() + "' already exists");
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
        Environment environment = findEnvironmentWithAccess(id, user, false);
        return environmentMapper.toResponse(environment);
    }

    @Override
    @Transactional
    public EnvironmentResponse updateEnvironment(Long id, EnvironmentRequest request, User user) {
        Environment environment = findEnvironmentWithAccess(id, user, true);

        environmentRepository.findByNameAndWorkspaceId(request.name(), environment.getWorkspace().getId())
                .ifPresent(existingEnv -> {
                    if (!existingEnv.getId().equals(id)) {
                        throw new IllegalArgumentException(
                                "Environment with name '" + request.name() + "' already exists");
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
        Environment environment = findEnvironmentWithAccess(id, user, true);
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
        Environment environment = findEnvironmentWithAccess(id, user, true);
        environment.setIsActive(isActive);
        Environment updatedEnvironment = environmentRepository.save(environment);
        return environmentMapper.toResponse(updatedEnvironment);
    }

    private Environment findEnvironmentWithAccess(Long id, User user, boolean requireWrite) {
        Environment environment = environmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Environment not found"));
        if (requireWrite) {
            workspaceAccessGuard.requireWrite(environment.getWorkspace(), user);
        } else {
            workspaceAccessGuard.requireRead(environment.getWorkspace(), user);
        }
        return environment;
    }
}