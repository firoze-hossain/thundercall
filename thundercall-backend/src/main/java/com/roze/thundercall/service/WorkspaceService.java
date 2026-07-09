package com.roze.thundercall.service;

import com.roze.thundercall.dto.OnboardingStep;
import com.roze.thundercall.dto.WorkspaceResponse;
import com.roze.thundercall.dto.WorkspaceSetupRequest;
import com.roze.thundercall.entity.TutorialStatus;
import com.roze.thundercall.entity.User;

import java.util.List;

public interface WorkspaceService {
    WorkspaceResponse setupInitialWorkspace(User user, WorkspaceSetupRequest request);

    boolean hasCompletedOnboarding(User user);

    TutorialStatus getTutorialStatus(User user);

    void markTutorialComplete(User user, String tutorialId);

    List<OnboardingStep> getOnboardingSteps(User user);

    List<WorkspaceResponse> getUserWorkspaces(User user);

    WorkspaceResponse getWorkspaceById(Long id, User user);
}
