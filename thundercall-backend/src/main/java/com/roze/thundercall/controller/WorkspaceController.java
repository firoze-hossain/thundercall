package com.roze.thundercall.controller;

import com.roze.thundercall.dto.OnboardingStep;
import com.roze.thundercall.dto.WorkspaceResponse;
import com.roze.thundercall.dto.WorkspaceSetupRequest;
import com.roze.thundercall.entity.User;
import com.roze.thundercall.service.AuthService;
import com.roze.thundercall.service.WorkspaceService;
import com.roze.thundercall.utils.BaseController;
import com.roze.thundercall.utils.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/workspaces")
public class WorkspaceController extends BaseController {
    private final WorkspaceService workspaceService;
    private final AuthService authService;

    @PostMapping("/setup")
    public ResponseEntity<BaseResponse<WorkspaceResponse>> setupWorkspace(@Valid @RequestBody WorkspaceSetupRequest request, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        WorkspaceResponse response = workspaceService.setupInitialWorkspace(user, request);
        return created(response, "Workspace created successfully");
    }

    @GetMapping("")
    public ResponseEntity<BaseResponse<List<WorkspaceResponse>>> getUserWorkspaces(Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        List<WorkspaceResponse> workspaceResponses = workspaceService.getUserWorkspaces(user);
        return ok(workspaceResponses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<WorkspaceResponse>> getWorkspace(@PathVariable Long id, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        WorkspaceResponse response = workspaceService.getWorkspaceById(id, user);
        return ok(response);
    }

    @GetMapping("/tutorial/status")
    public ResponseEntity<BaseResponse<Boolean>> getTutorialStatus(Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        boolean completed = workspaceService.hasCompletedOnboarding(user);
        return ok(completed);
    }

    @PostMapping("/tutorial/complete")
    public ResponseEntity<BaseResponse<Void>> markTutorialComplete(@RequestParam String stepId, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        workspaceService.markTutorialComplete(user, stepId);
        return noContent("Tutorial step completed");
    }

    @GetMapping("/tutorial/steps")
    public ResponseEntity<BaseResponse<List<OnboardingStep>>> getOnboardingSteps(Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        List<OnboardingStep> steps = workspaceService.getOnboardingSteps(user);
        return ok(steps);
    }

}
