package com.roze.thundercall.api.controller;

import com.roze.thundercall.api.dto.*;
import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.service.AuthService;
import com.roze.thundercall.api.service.TeamService;
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
@RequestMapping("/teams")
public class TeamController extends BaseController {
    private final TeamService teamService;
    private final AuthService authService;

    @PostMapping("")
    public ResponseEntity<BaseResponse<TeamResponse>> createTeam(
            @Valid @RequestBody TeamRequest request, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return created(teamService.createTeam(request, user), "Team created");
    }

    @GetMapping("")
    public ResponseEntity<BaseResponse<List<TeamResponse>>> getMyTeams(Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return ok(teamService.getMyTeams(user));
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<BaseResponse<TeamResponse>> getTeam(
            @PathVariable Long teamId, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return ok(teamService.getTeam(teamId, user));
    }

    @GetMapping("/{teamId}/members")
    public ResponseEntity<BaseResponse<List<TeamMemberResponse>>> getMembers(
            @PathVariable Long teamId, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return ok(teamService.getMembers(teamId, user));
    }

    @PostMapping("/{teamId}/invitations")
    public ResponseEntity<BaseResponse<TeamInvitationResponse>> inviteMember(
            @PathVariable Long teamId, @Valid @RequestBody InviteMemberRequest request, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return created(teamService.inviteMember(teamId, request, user), "Invitation sent to " + request.email());
    }

    @GetMapping("/{teamId}/invitations")
    public ResponseEntity<BaseResponse<List<TeamInvitationResponse>>> getPendingInvitations(
            @PathVariable Long teamId, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return ok(teamService.getPendingInvitations(teamId, user));
    }

    @DeleteMapping("/{teamId}/invitations/{invitationId}")
    public ResponseEntity<BaseResponse<Void>> revokeInvitation(
            @PathVariable Long teamId, @PathVariable Long invitationId, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        teamService.revokeInvitation(teamId, invitationId, user);
        return noContent("Invitation revoked");
    }

    /** Redeeming an invitation token — the user must already be logged in
     * (register + verify email first if they're brand new), then this
     * joins them to the team. */
    @PostMapping("/invitations/accept")
    public ResponseEntity<BaseResponse<TeamResponse>> acceptInvitation(
            @Valid @RequestBody AcceptInvitationRequest request, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return ok(teamService.acceptInvitation(request.token(), user), "You've joined the team");
    }

    @DeleteMapping("/{teamId}/members/{memberUserId}")
    public ResponseEntity<BaseResponse<Void>> removeMember(
            @PathVariable Long teamId, @PathVariable Long memberUserId, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        teamService.removeMember(teamId, memberUserId, user);
        return noContent("Member removed");
    }

    @PutMapping("/{teamId}/members/{memberUserId}/role")
    public ResponseEntity<BaseResponse<Void>> changeMemberRole(
            @PathVariable Long teamId, @PathVariable Long memberUserId,
            @Valid @RequestBody ChangeMemberRoleRequest request, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        teamService.changeMemberRole(teamId, memberUserId, request.role(), user);
        return noContent("Role updated");
    }

    @PostMapping("/{teamId}/leave")
    public ResponseEntity<BaseResponse<Void>> leaveTeam(
            @PathVariable Long teamId, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        teamService.leaveTeam(teamId, user);
        return noContent("You've left the team");
    }

    @DeleteMapping("/{teamId}")
    public ResponseEntity<BaseResponse<Void>> deleteTeam(
            @PathVariable Long teamId, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        teamService.deleteTeam(teamId, user);
        return noContent("Team deleted");
    }
}
