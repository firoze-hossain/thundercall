package com.roze.thundercall.api.service.impl;

import com.roze.thundercall.api.dto.MemberRoleEntry;
import com.roze.thundercall.api.dto.ShareWorkspaceRequest;
import com.roze.thundercall.api.dto.UpdateWorkspaceRoleRequest;
import com.roze.thundercall.api.dto.WorkspaceAccessResponse;
import com.roze.thundercall.api.entity.*;
import com.roze.thundercall.api.enums.TeamRole;
import com.roze.thundercall.api.enums.WorkspaceRole;
import com.roze.thundercall.api.exception.AuthException;
import com.roze.thundercall.api.exception.ResourceNotFoundException;
import com.roze.thundercall.api.repository.*;
import com.roze.thundercall.api.service.WorkspaceSharingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WorkspaceSharingServiceImpl implements WorkspaceSharingService {
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceAccessRepository workspaceAccessRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final CollectionRepository collectionRepository;
    private final FolderRepository folderRepository;
    private final RequestRepository requestRepository;
    private final EnvironmentRepository environmentRepository;
    private final com.roze.thundercall.api.mapper.CollectionMapper collectionMapper;
    private final com.roze.thundercall.api.mapper.FolderMapper folderMapper;
    private final com.roze.thundercall.api.mapper.EnvironmentMapper environmentMapper;
    private final RestTemplate restTemplate;

    @Override
    @Transactional
    public List<WorkspaceAccessResponse> shareWorkspace(Long workspaceId, ShareWorkspaceRequest request, User owner) {
        Workspace workspace = workspaceRepository.findByIdAndOwner(workspaceId, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        Team team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        TeamMember requesterMembership = teamMemberRepository.findByTeamAndUser(team, owner)
                .orElseThrow(() -> new AuthException("You're not a member of that team"));
        if (requesterMembership.getRole() != TeamRole.OWNER && requesterMembership.getRole() != TeamRole.ADMIN) {
            throw new AuthException("Only a team Owner or Admin can share a workspace with the team");
        }

        for (MemberRoleEntry entry : request.members()) {
            User member = userRepository.findById(entry.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + entry.userId()));
            if (member.getId().equals(owner.getId())) {
                continue; // the owner always has full access — no access row needed for themselves
            }
            if (!teamMemberRepository.existsByTeamAndUser(team, member)) {
                throw new AuthException(member.getUsername() + " isn't a member of that team");
            }
            WorkspaceAccess access = workspaceAccessRepository.findByWorkspaceAndUser(workspace, member)
                    .orElse(WorkspaceAccess.builder().workspace(workspace).user(member).build());
            access.setTeam(team);
            access.setRole(entry.role());
            // Explicit opt-in only — an environment ID that doesn't
            // actually belong to this workspace is silently ignored
            // rather than trusted, so there's no way to smuggle in
            // access to someone else's environment via a crafted ID.
            access.getAllowedEnvironments().clear();
            if (entry.environmentIds() != null && !entry.environmentIds().isEmpty()) {
                environmentRepository.findAllById(entry.environmentIds()).stream()
                        .filter(env -> env.getWorkspace().getId().equals(workspaceId))
                        .forEach(env -> access.getAllowedEnvironments().add(env));
            }
            workspaceAccessRepository.save(access);
        }

        return getWorkspaceAccessList(workspaceId, owner);
    }

    @Override
    public List<WorkspaceAccessResponse> getSharedWithMe(User user) {
        return workspaceAccessRepository.findByUserOrderByGrantedAtDesc(user).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<WorkspaceAccessResponse> getWorkspaceAccessList(Long workspaceId, User owner) {
        Workspace workspace = workspaceRepository.findByIdAndOwner(workspaceId, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
        return workspaceAccessRepository.findByWorkspaceOrderByGrantedAtDesc(workspace).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public WorkspaceAccessResponse updateAccessRole(Long workspaceId, Long userId, UpdateWorkspaceRoleRequest request, User owner) {
        Workspace workspace = workspaceRepository.findByIdAndOwner(workspaceId, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
        User member = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        WorkspaceAccess access = workspaceAccessRepository.findByWorkspaceAndUser(workspace, member)
                .orElseThrow(() -> new ResourceNotFoundException("That user doesn't have access to this workspace"));
        access.setRole(request.role());
        return toResponse(workspaceAccessRepository.save(access));
    }

    @Override
    @Transactional
    public WorkspaceAccessResponse updateAccessEnvironments(Long workspaceId, Long userId, List<Long> environmentIds, User owner) {
        Workspace workspace = workspaceRepository.findByIdAndOwner(workspaceId, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
        User member = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        WorkspaceAccess access = workspaceAccessRepository.findByWorkspaceAndUser(workspace, member)
                .orElseThrow(() -> new ResourceNotFoundException("That user doesn't have access to this workspace"));
        access.getAllowedEnvironments().clear();
        if (environmentIds != null && !environmentIds.isEmpty()) {
            environmentRepository.findAllById(environmentIds).stream()
                    .filter(env -> env.getWorkspace().getId().equals(workspaceId))
                    .forEach(env -> access.getAllowedEnvironments().add(env));
        }
        return toResponse(workspaceAccessRepository.save(access));
    }

    @Override
    @Transactional
    public void revokeAccess(Long workspaceId, Long userId, User owner) {
        Workspace workspace = workspaceRepository.findByIdAndOwner(workspaceId, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
        User member = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        workspaceAccessRepository.deleteByWorkspaceAndUser(workspace, member);
    }

    @Override
    public boolean hasAccess(Long workspaceId, User user, WorkspaceRole minimumRole) {
        Optional<Workspace> workspace = workspaceRepository.findById(workspaceId);
        if (workspace.isEmpty()) {
            return false;
        }
        if (workspace.get().getOwner().getId().equals(user.getId())) {
            return true; // owners always have full access
        }
        Optional<WorkspaceAccess> access = workspaceAccessRepository.findByWorkspaceAndUser(workspace.get(), user);
        if (access.isEmpty()) {
            return false;
        }
        // EDITOR is a strict superset of VIEWER — anything a Viewer can do,
        // an Editor can do too, so an EDITOR row satisfies a VIEWER check.
        return minimumRole == WorkspaceRole.VIEWER || access.get().getRole() == WorkspaceRole.EDITOR;
    }

    @Override
    @Transactional
    public com.roze.thundercall.api.dto.WorkspaceContentsResponse getWorkspaceContents(Long workspaceId, User user) {
        if (!hasAccess(workspaceId, user, WorkspaceRole.VIEWER)) {
            throw new AuthException("You don't have access to this workspace");
        }
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        // FIX: this used to fetch folders and requests ONE COLLECTION AT
        // A TIME in a loop, plus a separate COUNT query per folder for
        // its request count — a classic N+1 problem. A workspace with 5
        // collections and 10 folders each was firing 60+ separate
        // database round-trips just to open the browse dialog, which is
        // exactly why it felt slow. Everything below is now 4 bulk
        // queries total, regardless of how many collections/folders/
        // requests actually exist, with the grouping done in memory.
        List<Collection> collections = collectionRepository.findByWorkspaceId(workspaceId);
        List<Long> collectionIds = collections.stream().map(Collection::getId).toList();

        List<Folder> allFolders = collectionIds.isEmpty()
                ? List.of() : folderRepository.findByCollectionIdIn(collectionIds);
        List<Request> allRequests = collectionIds.isEmpty()
                ? List.of() : requestRepository.findByCollectionIdIn(collectionIds);

        List<Long> folderIds = allFolders.stream().map(Folder::getId).toList();
        Map<Long, Long> requestCountByFolderId = new java.util.HashMap<>();
        if (!folderIds.isEmpty()) {
            for (Object[] row : folderRepository.countRequestsByFolderIds(folderIds)) {
                requestCountByFolderId.put((Long) row[0], (Long) row[1]);
            }
        }

        Map<Long, List<Folder>> foldersByCollectionId = allFolders.stream()
                .collect(java.util.stream.Collectors.groupingBy(f -> f.getCollection().getId()));
        Map<Long, List<Request>> requestsByCollectionId = allRequests.stream()
                .collect(java.util.stream.Collectors.groupingBy(r -> r.getCollection().getId()));

        List<com.roze.thundercall.api.dto.CollectionResponse> collectionResponses = collections.stream()
                .map(collection -> {
                    List<com.roze.thundercall.api.dto.FolderResponse> folderResponses =
                            foldersByCollectionId.getOrDefault(collection.getId(), List.of()).stream()
                                    .map(folder -> {
                                        com.roze.thundercall.api.dto.FolderResponse fr = folderMapper.toResponse(folder);
                                        fr.setRequestCount(requestCountByFolderId.getOrDefault(folder.getId(), 0L).intValue());
                                        return fr;
                                    })
                                    .toList();
                    List<com.roze.thundercall.api.dto.RequestResponse> requestResponses =
                            requestsByCollectionId.getOrDefault(collection.getId(), List.of()).stream()
                                    .map(request -> com.roze.thundercall.api.dto.RequestResponse.builder()
                                            .id(request.getId())
                                            .name(request.getName())
                                            .description(request.getDescription())
                                            .method(request.getMethod())
                                            .url(request.getUrl())
                                            .headers(request.getHeaders())
                                            .body(request.getBody())
                                            .collectionId(collection.getId())
                                            .collectionName(collection.getName())
                                            .folderId(request.getFolder() != null ? request.getFolder().getId() : null)
                                            .folderName(request.getFolder() != null ? request.getFolder().getName() : null)
                                            .createdAt(request.getCreatedAt())
                                            .updatedAt(request.getUpdatedAt())
                                            .build())
                                    .toList();
                    return collectionMapper.toDetailedResponse(collection, folderResponses, requestResponses);
                })
                .toList();

        // Environments are opt-in only, unlike collections — the owner
        // sees everything, but anyone else only sees the specific
        // environments they've been explicitly granted (see
        // WorkspaceAccess.allowedEnvironments). No access row at all
        // (shouldn't happen here, since hasAccess() already passed)
        // would also mean zero environments, which is the correct
        // fail-safe default.
        List<Environment> visibleEnvironments;
        if (workspace.getOwner().getId().equals(user.getId())) {
            visibleEnvironments = environmentRepository.findByWorkspaceId(workspaceId);
        } else {
            visibleEnvironments = workspaceAccessRepository.findByWorkspaceAndUser(workspace, user)
                    .map(access -> access.getAllowedEnvironments().stream().toList())
                    .orElse(List.of());
        }
        List<com.roze.thundercall.api.dto.EnvironmentResponse> environmentResponses =
                visibleEnvironments.stream()
                        .map(environmentMapper::toResponse)
                        .toList();

        return new com.roze.thundercall.api.dto.WorkspaceContentsResponse(
                workspace.getId(), workspace.getName(), workspace.getOwner().getUsername(),
                collectionResponses, environmentResponses);
    }

    @Override
    @Transactional
    public com.roze.thundercall.api.dto.ApiResponse executeSharedRequest(
            Long workspaceId, Long requestId, com.roze.thundercall.api.dto.ApiRequest overrides, User user) {
        if (!hasAccess(workspaceId, user, WorkspaceRole.EDITOR)) {
            throw new AuthException("Editor access is required to send requests in this workspace");
        }
        Request request = requestRepository.findByIdAndWorkspaceId(requestId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found in this workspace"));

        // Overrides let the caller tweak the URL/headers/body before
        // sending (same as editing a normal request tab) without
        // persisting those changes back to the owner's saved request —
        // falls back to what's actually saved if nothing was overridden.
        String url = overrides != null && overrides.url() != null && !overrides.url().isBlank()
                ? overrides.url() : request.getUrl();
        String headersJson = overrides != null && overrides.headers() != null ? overrides.headers() : request.getHeaders();
        String body = overrides != null && overrides.body() != null ? overrides.body() : request.getBody();

        Instant startTime = Instant.now();
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        if (headersJson != null && !headersJson.isBlank()) {
            try {
                Map<String, String> headerMap = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                        headersJson, new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {
                        });
                headerMap.forEach((k, v) -> {
                    if (k != null && !k.isBlank() && v != null) {
                        headers.add(k.trim(), v.trim());
                    }
                });
            } catch (Exception ignored) {
                // malformed headers JSON — send without them rather than failing outright
            }
        }
        try {
            org.springframework.http.HttpEntity<String> entity =
                    new org.springframework.http.HttpEntity<>(body != null && !body.isBlank() ? body : null, headers);
            org.springframework.http.ResponseEntity<byte[]> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.valueOf(request.getMethod().name()), entity, byte[].class);
            long duration = Duration.between(startTime, Instant.now()).toMillis();
            byte[] responseBody = response.getBody() != null ? response.getBody() : new byte[0];
            return com.roze.thundercall.api.dto.ApiResponse.builder()
                    .statusCode(response.getStatusCode().value())
                    .response(new String(responseBody, java.nio.charset.StandardCharsets.UTF_8))
                    .responseHeaders(response.getHeaders().toString())
                    .duration(duration)
                    .success(!response.getStatusCode().isError())
                    .binary(false)
                    .sizeBytes(responseBody.length)
                    .build();
        } catch (Exception e) {
            long duration = Duration.between(startTime, Instant.now()).toMillis();
            return com.roze.thundercall.api.dto.ApiResponse.builder()
                    .statusCode(0)
                    .response("Request failed: " + e.getMessage())
                    .duration(duration)
                    .success(false)
                    .binary(false)
                    .build();
        }
    }

    private WorkspaceAccessResponse toResponse(WorkspaceAccess access) {
        List<Long> envIds = access.getAllowedEnvironments().stream().map(Environment::getId).toList();
        List<String> envNames = access.getAllowedEnvironments().stream().map(Environment::getName).toList();
        return new WorkspaceAccessResponse(
                access.getId(),
                access.getWorkspace().getId(), access.getWorkspace().getName(),
                access.getWorkspace().getOwner().getUsername(),
                access.getUser().getId(), access.getUser().getUsername(), access.getUser().getEmail(),
                // FIX: team is nullable — access granted through a direct
                // email invite (the primary flow now) has no team at all.
                // The unconditional access.getTeam().getId() this used to
                // do threw a NullPointerException for every one of those,
                // which meant "Team Spaces" and "Current Access" both
                // failed to load for anyone invited this way.
                access.getTeam() != null ? access.getTeam().getId() : null,
                access.getTeam() != null ? access.getTeam().getName() : null,
                access.getRole(), envIds, envNames, access.getGrantedAt());
    }
}
