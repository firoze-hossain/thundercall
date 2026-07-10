package com.roze.thundercall.api.service.impl;

import com.roze.thundercall.api.dto.OnboardingStep;
import com.roze.thundercall.api.dto.WorkspaceResponse;
import com.roze.thundercall.api.dto.WorkspaceSetupRequest;
import com.roze.thundercall.api.entity.*;
import com.roze.thundercall.api.enums.HttpMethod;
import com.roze.thundercall.api.exception.ResourceNotFoundException;
import com.roze.thundercall.api.mapper.TutorialStatusMapper;
import com.roze.thundercall.api.mapper.WorkspaceMapper;
import com.roze.thundercall.api.repository.CollectionRepository;
import com.roze.thundercall.api.repository.RequestRepository;
import com.roze.thundercall.api.repository.WorkspaceRepository;
import com.roze.thundercall.api.service.TutorialStatusService;
import com.roze.thundercall.api.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * FIXES over the previous version:
 *  1. getOrCreateDefaultWorkspace(): every user is guaranteed a workspace.
 *     Called automatically at registration (AuthServiceImpl) and defensively
 *     by CollectionServiceImpl / EnvironmentServiceImpl, so existing users
 *     who registered before this fix are healed on their next action.
 *     Result: "No workspace found" can never reach the UI.
 *  2. Sample request headers were broken JSON: the old code produced the
 *     literal string {\"Content-Type\": \"application/json\"} (with real
 *     backslashes) and the sample body was wrapped in stray quote marks.
 *     Both are now valid JSON.
 */
@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {
    private final WorkspaceRepository workspaceRepository;
    private final RequestRepository requestRepository;
    private final CollectionRepository collectionRepository;
    private final TutorialStatusService tutorialStatusService;
    private final WorkspaceMapper workspaceMapper;
    private final TutorialStatusMapper tutorialStatusMapper;

    @Override
    @Transactional
    public WorkspaceResponse setupInitialWorkspace(User user, WorkspaceSetupRequest request) {
        // FIX (HTTP 409): users can create additional workspaces, like
        // Postman. The old code threw "User already has workspace" — but
        // since registration now auto-creates a default workspace, that
        // made "Create Workspace" impossible for everyone.
        Workspace workspace = createWorkspace(user,
                request.getWorkspaceName(),
                request.getCreateSampleData() == null || request.getCreateSampleData());
        tutorialStatusService.getOrCreateTutorialStatus(user);
        return workspaceMapper.toResponse(workspace);
    }

    @Override
    @Transactional
    public Workspace getOrCreateDefaultWorkspace(User user) {
        return workspaceRepository.findByOwner(user)
                .stream()
                .findFirst()
                .orElseGet(() -> createWorkspace(user, null, true));
    }

    private Workspace createWorkspace(User user, String name, boolean withSampleData) {
        Workspace workspace = Workspace.builder()
                .name(name != null && !name.isBlank()
                        ? name
                        : user.getUsername() + "'s Workspace")
                .description("Default workspace")
                .owner(user)
                .collections(new ArrayList<>())
                .build();
        workspace = workspaceRepository.save(workspace);

        if (withSampleData) {
            Collection defaultCollection = createDefaultCollection(workspace);
            workspace.getCollections().add(defaultCollection);
            workspace = workspaceRepository.save(workspace);
        }
        return workspace;
    }

    private Collection createDefaultCollection(Workspace workspace) {
        Collection collection = Collection.builder()
                .name("Getting started")
                .description("Sample requests to help you get started")
                .workspace(workspace)
                .requests(new ArrayList<>())
                .build();

        collection = collectionRepository.save(collection);
        List<Request> sampleRequests = createSampleRequests(collection);
        requestRepository.saveAll(sampleRequests);
        collection.getRequests().addAll(sampleRequests);
        return collectionRepository.save(collection);
    }

    private List<Request> createSampleRequests(Collection collection) {
        List<Request> requests = new ArrayList<>();

        Request getRequest = Request.builder()
                .name("Get Users Example")
                .method(HttpMethod.GET)
                .url("https://jsonplaceholder.typicode.com/users")
                .collection(collection)
                .description("Example GET request to fetch users")
                .headers("{\"Accept\": \"application/json\"}")
                .build();

        Request postRequest = Request.builder()
                .name("Post Create User")
                .method(HttpMethod.POST)
                .url("https://jsonplaceholder.typicode.com/users")
                .body("{\n" +
                        "  \"name\": \"John Doe\",\n" +
                        "  \"email\": \"john.doe@example.com\",\n" +
                        "  \"username\": \"johndoe\"\n" +
                        "}")
                .collection(collection)
                .description("Example POST request to create a user")
                .headers("{\"Content-Type\": \"application/json\"}")
                .build();

        requests.add(getRequest);
        requests.add(postRequest);
        return requests;
    }

    @Override
    public boolean hasCompletedOnboarding(User user) {
        return tutorialStatusService.isTutorialCompleted(user);
    }

    @Override
    public TutorialStatus getTutorialStatus(User user) {
        return tutorialStatusService.getOrCreateTutorialStatus(user);
    }

    @Override
    public void markTutorialComplete(User user, String tutorialId) {
        tutorialStatusService.markStepComplete(user, tutorialId);
    }

    @Override
    public List<OnboardingStep> getOnboardingSteps(User user) {
        TutorialStatus status = getTutorialStatus(user);
        return tutorialStatusMapper.toOnboardingSteps(status);
    }

    @Override
    public List<WorkspaceResponse> getUserWorkspaces(User user) {
        return workspaceRepository.findByOwner(user).stream()
                .map(workspaceMapper::toResponse)
                .toList();
    }

    @Override
    public WorkspaceResponse getWorkspaceById(Long id, User user) {
        Workspace workspace = workspaceRepository.findByIdAndOwner(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
        return workspaceMapper.toResponse(workspace);
    }
}