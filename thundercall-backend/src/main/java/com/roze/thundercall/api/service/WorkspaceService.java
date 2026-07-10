package com.roze.thundercall.api.service;

import com.roze.thundercall.api.dto.OnboardingStep;
import com.roze.thundercall.api.dto.WorkspaceResponse;
import com.roze.thundercall.api.dto.WorkspaceSetupRequest;
import com.roze.thundercall.api.entity.TutorialStatus;
import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.entity.Workspace;

import java.util.List;

public interface WorkspaceService {
    WorkspaceResponse setupInitialWorkspace(User user, WorkspaceSetupRequest request);

    /**
     * Returns the user's workspace, creating a default one automatically if
     * none exists. This is the key to the "just works after login" flow:
     * no service should ever throw "No workspace found" at the user.
     */
    Workspace getOrCreateDefaultWorkspace(User user);

    boolean hasCompletedOnboarding(User user);

    TutorialStatus getTutorialStatus(User user);

    void markTutorialComplete(User user, String tutorialId);

    List<OnboardingStep> getOnboardingSteps(User user);

    List<WorkspaceResponse> getUserWorkspaces(User user);

    WorkspaceResponse getWorkspaceById(Long id, User user);
}