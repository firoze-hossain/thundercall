package com.roze.thundercall.api.controller;

import com.roze.thundercall.api.dto.ShareWorkspaceRequest;
import com.roze.thundercall.api.dto.UpdateWorkspaceRoleRequest;
import com.roze.thundercall.api.dto.WorkspaceAccessResponse;
import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.service.AuthService;
import com.roze.thundercall.api.service.WorkspaceSharingService;
import com.roze.thundercall.api.utils.BaseController;
import com.roze.thundercall.api.utils.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/workspace-sharing")
public class WorkspaceSharingController extends BaseController {
    private final WorkspaceSharingService workspaceSharingService;
    private final AuthService authService;

    @PostMapping("/{workspaceId}/share")
    public ResponseEntity<BaseResponse<List<WorkspaceAccessResponse>>> shareWorkspace(
            @PathVariable Long workspaceId, @Valid @RequestBody ShareWorkspaceRequest request,
            Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return ok(workspaceSharingService.shareWorkspace(workspaceId, request, user), "Workspace shared");
    }

    @GetMapping("/shared-with-me")
    public ResponseEntity<BaseResponse<List<WorkspaceAccessResponse>>> getSharedWithMe(Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return ok(workspaceSharingService.getSharedWithMe(user));
    }

    @GetMapping("/{workspaceId}/access")
    public ResponseEntity<BaseResponse<List<WorkspaceAccessResponse>>> getWorkspaceAccessList(
            @PathVariable Long workspaceId, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return ok(workspaceSharingService.getWorkspaceAccessList(workspaceId, user));
    }

    @PutMapping("/{workspaceId}/access/{userId}")
    public ResponseEntity<BaseResponse<WorkspaceAccessResponse>> updateAccessRole(
            @PathVariable Long workspaceId, @PathVariable Long userId,
            @Valid @RequestBody UpdateWorkspaceRoleRequest request, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return ok(workspaceSharingService.updateAccessRole(workspaceId, userId, request, user), "Role updated");
    }

    @PutMapping("/{workspaceId}/access/{userId}/environments")
    public ResponseEntity<BaseResponse<WorkspaceAccessResponse>> updateAccessEnvironments(
            @PathVariable Long workspaceId, @PathVariable Long userId,
            @RequestBody List<Long> environmentIds, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return ok(workspaceSharingService.updateAccessEnvironments(workspaceId, userId, environmentIds, user),
                "Environments updated");
    }

    @DeleteMapping("/{workspaceId}/access/{userId}")
    public ResponseEntity<BaseResponse<Void>> revokeAccess(
            @PathVariable Long workspaceId, @PathVariable Long userId, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        workspaceSharingService.revokeAccess(workspaceId, userId, user);
        return noContent("Access revoked");
    }

    @GetMapping("/{workspaceId}/contents")
    public ResponseEntity<BaseResponse<com.roze.thundercall.api.dto.WorkspaceContentsResponse>> getWorkspaceContents(
            @PathVariable Long workspaceId, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return ok(workspaceSharingService.getWorkspaceContents(workspaceId, user));
    }

    @PostMapping("/{workspaceId}/requests/{requestId}/execute")
    public ResponseEntity<BaseResponse<com.roze.thundercall.api.dto.ApiResponse>> executeSharedRequest(
            @PathVariable Long workspaceId, @PathVariable Long requestId,
            @RequestBody(required = false) com.roze.thundercall.api.dto.ApiRequest overrides,
            Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return ok(workspaceSharingService.executeSharedRequest(workspaceId, requestId, overrides, user));
    }
}
