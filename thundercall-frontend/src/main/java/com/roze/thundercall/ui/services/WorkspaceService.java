package com.roze.thundercall.ui.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.roze.thundercall.ui.models.BaseResponse;
import com.roze.thundercall.ui.models.OnboardingStep;
import com.roze.thundercall.ui.models.Workspace;
import com.roze.thundercall.ui.models.WorkspaceSetupRequest;
import com.roze.thundercall.ui.utils.AlertUtils;
import javafx.application.Platform;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class WorkspaceService {
    private static final String BASE_URL = "/workspaces";

    public static Optional<Workspace> setupInitialWorkspace(String workspaceName) {
        return setupInitialWorkspace(workspaceName, true);
    }

    public static Optional<Workspace> setupInitialWorkspace(String workspaceName, boolean createSampleData) {
        WorkspaceSetupRequest request = new WorkspaceSetupRequest(workspaceName);
        request.setCreateSampleData(createSampleData);
        try {
            BaseResponse<Workspace> response = ApiClient.post(BASE_URL + "/setup", request, new TypeReference<BaseResponse<Workspace>>() {
            });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            Platform.runLater(() -> AlertUtils.showError("Failed to setup workspace: " + e.getMessage()));
        }
        return Optional.empty();
    }

    /** Fetches all workspaces of the logged-in user (GET /workspaces). */
    public static Optional<List<Workspace>> getUserWorkspaces() {
        try {
            BaseResponse<List<Workspace>> response = ApiClient.get(BASE_URL,
                    new TypeReference<BaseResponse<List<Workspace>>>() {
                    });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            System.out.println("Failed to load workspaces: " + e.getMessage());
        }
        return Optional.empty();
    }

    public static boolean checkTutorialStatus() {
        try {
            BaseResponse<Boolean> response = ApiClient.get(BASE_URL + "/tutorial/status", new TypeReference<BaseResponse<Boolean>>() {
            });
            return response != null && response.isSuccess() && Boolean.TRUE.equals(response.getData());
        } catch (IOException e) {
            System.out.println("Failed to check tutorial status: " + e.getMessage());
            return false;
        }
    }

    public static List<OnboardingStep> getOnboardingSteps() {
        try {
            BaseResponse<List<OnboardingStep>> response = ApiClient.get(BASE_URL + "/tutorial/steps", new TypeReference<BaseResponse<List<OnboardingStep>>>() {
            });
            if (response != null && response.isSuccess()) {
                return response.getData();
            }
        } catch (IOException e) {
            System.out.printf("Failed to get onboarding steps: " + e.getMessage());
        }
        return getDefaultOnboardingSteps();
    }

    public static void markTutorialComplete(String stepId) {
        try {
            ApiClient.post(BASE_URL + "/tutorial/complete?stepId=" + stepId, null, new TypeReference<BaseResponse<Void>>() {
            });
        } catch (IOException e) {
            System.out.printf("Failed to mark tutorial complete: " + e.getMessage());
        }
    }

    public static List<OnboardingStep> getDefaultOnboardingSteps() {
        return List.of(

                new OnboardingStep("welcome", "Welcome to ThunderCall",
                        "Let's explore the powerful features of your API testing tool", 1, false),
                new OnboardingStep("request-builder", "Request Builder",
                        "Create and customize HTTP requests with our intuitive builder", 2, false),
                new OnboardingStep("collections", "Collections",
                        "Organize your requests into collections for better management", 3, false),
                new OnboardingStep("environments", "Environments",
                        "Manage different deployment environments with variables", 4, false),
                new OnboardingStep("testing", "API Testing",
                        "Write and execute tests for your APIs", 5, false),
                new OnboardingStep("history", "Request History",
                        "Track all your API calls with detailed history", 6, false)
        );
    }
}