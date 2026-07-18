package com.roze.thundercall.api.controller;

import com.roze.thundercall.api.dto.AcceptInvitationRequest;
import com.roze.thundercall.api.dto.InviteToWorkspaceRequest;
import com.roze.thundercall.api.dto.WorkspaceAccessResponse;
import com.roze.thundercall.api.dto.WorkspaceInvitationResponse;
import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.service.AuthService;
import com.roze.thundercall.api.service.WorkspaceInvitationService;
import com.roze.thundercall.api.utils.BaseController;
import com.roze.thundercall.api.utils.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** The simple, direct workspace-sharing flow — invite by email, no
 * team required. See WorkspaceSharingController for the team-based
 * alternative, still available for sharing with a whole team at once. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/workspace-invitations")
public class WorkspaceInvitationController extends BaseController {
    private final WorkspaceInvitationService workspaceInvitationService;
    private final AuthService authService;

    @PostMapping("/{workspaceId}/invite")
    public ResponseEntity<BaseResponse<WorkspaceInvitationResponse>> inviteToWorkspace(
            @PathVariable Long workspaceId, @Valid @RequestBody InviteToWorkspaceRequest request,
            Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return created(workspaceInvitationService.inviteToWorkspace(workspaceId, request, user), "Invitation sent");
    }

    @PostMapping("/{workspaceId}/invite-team")
    public ResponseEntity<BaseResponse<List<WorkspaceInvitationResponse>>> inviteTeamMembers(
            @PathVariable Long workspaceId, @Valid @RequestBody com.roze.thundercall.api.dto.InviteTeamRequest request,
            Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return created(workspaceInvitationService.inviteTeamMembers(workspaceId, request, user), "Invitations sent");
    }

    @GetMapping("/{workspaceId}/pending")
    public ResponseEntity<BaseResponse<List<WorkspaceInvitationResponse>>> getPendingInvitations(
            @PathVariable Long workspaceId, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return ok(workspaceInvitationService.getPendingInvitations(workspaceId, user));
    }

    @DeleteMapping("/{workspaceId}/{invitationId}")
    public ResponseEntity<BaseResponse<Void>> revokeInvitation(
            @PathVariable Long workspaceId, @PathVariable Long invitationId, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        workspaceInvitationService.revokeInvitation(workspaceId, invitationId, user);
        return noContent("Invitation revoked");
    }

    @PostMapping("/accept")
    public ResponseEntity<BaseResponse<WorkspaceAccessResponse>> acceptInvitation(
            @Valid @RequestBody AcceptInvitationRequest request, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return ok(workspaceInvitationService.acceptInvitation(request.token(), user), "Invitation accepted");
    }
}
