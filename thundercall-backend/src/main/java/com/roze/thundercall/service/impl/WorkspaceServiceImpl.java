package com.roze.thundercall.service.impl;

import com.roze.thundercall.dto.OnboardingStep;
import com.roze.thundercall.dto.WorkspaceResponse;
import com.roze.thundercall.dto.WorkspaceSetupRequest;
import com.roze.thundercall.entity.*;
import com.roze.thundercall.enums.HttpMethod;
import com.roze.thundercall.exception.ResourceExistException;
import com.roze.thundercall.exception.ResourceNotFoundException;
import com.roze.thundercall.mapper.TutorialStatusMapper;
import com.roze.thundercall.mapper.WorkspaceMapper;
import com.roze.thundercall.repository.CollectionRepository;
import com.roze.thundercall.repository.RequestRepository;
import com.roze.thundercall.repository.WorkspaceRepository;
import com.roze.thundercall.service.TutorialStatusService;
import com.roze.thundercall.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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
        if (workspaceRepository.existsByOwner(user)) {
            throw new ResourceExistException("User already has workspace");
        }
        // Debug: Check user ID
        if (user.getId() == null) {
            throw new IllegalArgumentException("User must have a valid ID");
        }
        Workspace workspace = Workspace.builder()
                .name(request.getWorkspaceName() != null ?
                        request.getWorkspaceName() : user.getUsername() + "'s Workspace")
                .description("Default workspace")
                .owner(user)
                .collections(new ArrayList<>())
                .build();
        workspaceRepository.save(workspace);
        if (request.getCreateSampleData() == null || request.getCreateSampleData()) {
            Collection defaultCollection = createDefaultCollection(workspace);
            collectionRepository.save(defaultCollection);
            workspace.getCollections().add(defaultCollection);
            workspaceRepository.save(workspace);
        }
        tutorialStatusService.getOrCreateTutorialStatus(user);

        return workspaceMapper.toResponse(workspace);
    }

    @Override
    public boolean hasCompletedOnboarding(User user) {
        return tutorialStatusService.isTutorialCompleted(user);
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
                .headers("{\\\"Content-Type\\\": \\\"application/json\\\"}")
                .build();

        Request postRequest = Request.builder()
                .name("Post Create User")
                .method(HttpMethod.POST)
                .url("https://jsonplaceholder.typicode.com/users")
                .body("\"\"\n" +
                        "                    {\n" +
                        "                        \"name\": \"John Doe\",\n" +
                        "                        \"email\": \"john.doe@example.com\",\n" +
                        "                        \"username\": \"johndoe\"\n" +
                        "                    }\n" +
                        "                    \"\"")
                .collection(collection)
                .description("Example POST request to create a user")
                .headers("{\\\"Content-Type\\\": \\\"application/json\\\"}")
                .build();
        requests.add(getRequest);
        requests.add(postRequest);
        return requests;

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
        return workspaceRepository.findByOwner(user).stream().map(workspaceMapper::toResponse)
                .toList();
    }

    @Override
    public WorkspaceResponse getWorkspaceById(Long id, User user) {
        Workspace workspace = workspaceRepository.findByIdAndOwner(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
        return workspaceMapper.toResponse(workspace);
    }
}
