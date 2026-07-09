package com.roze.thundercall.mapper;

import com.roze.thundercall.dto.OnboardingStep;
import com.roze.thundercall.entity.TutorialStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TutorialStatusMapper {
    public List<OnboardingStep> toOnboardingSteps(TutorialStatus tutorialStatus) {
        List<String> completedSteps = tutorialStatus.getCompletedSteps();
        return List.of(
                new OnboardingStep("welcome", "Welcome to ThunderCall",
                        "Let's explore the powerful features of your API testing tool", 1,
                        completedSteps.contains("welcome")),
                new OnboardingStep("request-builder", "Request Builder",
                        "Create and customize HTTP requests with our intuitive builder", 2,
                        completedSteps.contains("request-builder")),
                new OnboardingStep("collections", "Collections",
                        "Organize your requests into collections for better management", 3,
                        completedSteps.contains("collections")),
                new OnboardingStep("environments", "Environments",
                        "Manage different deployment environments with variables", 4,
                        completedSteps.contains("environments")),
                new OnboardingStep("testing", "API Testing",
                        "Write and execute tests for your APIs", 5,
                        completedSteps.contains("testing")),
                new OnboardingStep("history", "Request History",
                        "Track all your API calls with detailed history", 6,
                        completedSteps.contains("history"))

        );
    }
}
