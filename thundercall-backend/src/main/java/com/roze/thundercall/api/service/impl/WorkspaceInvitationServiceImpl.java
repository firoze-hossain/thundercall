package com.roze.thundercall.api.service.impl;

import com.roze.thundercall.api.dto.InviteTeamRequest;
import com.roze.thundercall.api.dto.InviteToWorkspaceRequest;
import com.roze.thundercall.api.dto.WorkspaceAccessResponse;
import com.roze.thundercall.api.dto.WorkspaceInvitationResponse;
import com.roze.thundercall.api.entity.*;
import com.roze.thundercall.api.enums.InvitationStatus;
import com.roze.thundercall.api.exception.AuthException;
import com.roze.thundercall.api.exception.ResourceNotFoundException;
import com.roze.thundercall.api.repository.*;
import com.roze.thundercall.api.service.EmailService;
import com.roze.thundercall.api.service.WorkspaceInvitationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/** The simple, direct way to share a workspace — see the interface for
 * why this exists alongside the team-based WorkspaceSharingService. */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceInvitationServiceImpl implements WorkspaceInvitationService {
    private static final int INVITATION_DAYS_VALID = 7;

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceInvitationRepository workspaceInvitationRepository;
    private final WorkspaceAccessRepository workspaceAccessRepository;
    private final UserRepository userRepository;
    private final EnvironmentRepository environmentRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public WorkspaceInvitationResponse inviteToWorkspace(Long workspaceId, InviteToWorkspaceRequest request, User owner) {
        Workspace workspace = workspaceRepository.findByIdAndOwner(workspaceId, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        WorkspaceInvitation invitation = createInvitation(
                workspace, request.email(), request.role(), request.environmentIds(), owner);
        sendInvitationEmail(invitation, workspace, owner);
        return toResponse(invitation);
    }

    @Override
    @Transactional
    public List<WorkspaceInvitationResponse> inviteTeamMembers(Long workspaceId, InviteTeamRequest request, User owner) {
        Workspace workspace = workspaceRepository.findByIdAndOwner(workspaceId, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
        Team team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));

        List<WorkspaceInvitationResponse> results = new ArrayList<>();
        for (TeamMember member : teamMemberRepository.findByTeam(team)) {
            User memberUser = member.getUser();
            if (memberUser.getId().equals(owner.getId())) {
                continue; // never invite yourself to your own workspace
            }
            if (workspaceAccessRepository.findByWorkspaceAndUser(workspace, memberUser).isPresent()) {
                continue; // already has access — nothing to do for this one
            }
            if (workspaceInvitationRepository.findByWorkspaceAndEmailIgnoreCaseAndStatus(
                    workspace, memberUser.getEmail(), InvitationStatus.PENDING).isPresent()) {
                continue; // already invited and still waiting on them
            }
            try {
                WorkspaceInvitation invitation = createInvitation(
                        workspace, memberUser.getEmail(), request.role(), request.environmentIds(), owner);
                sendInvitationEmail(invitation, workspace, owner);
                results.add(toResponse(invitation));
            } catch (Exception e) {
                // One member's email failing to send (bad mail config,
                // etc.) shouldn't block inviting the rest of the team —
                // log it and keep going; the owner can retry that one
                // person individually via the single-email invite.
                log.warn("Couldn't invite team member {} to workspace {}: {}",
                        memberUser.getEmail(), workspaceId, e.getMessage());
            }
        }
        return results;
    }

    /** Shared by the single-email and whole-team invite paths — one
     * consistent way an invitation actually gets created, so both
     * paths behave identically (same duplicate checks, same expiry,
     * same environment handling) rather than two flows that could
     * quietly drift apart from each other over time. */
    private WorkspaceInvitation createInvitation(
            Workspace workspace, String email, com.roze.thundercall.api.enums.WorkspaceRole role,
            List<Long> environmentIds, User owner) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(existingUser -> {
            if (workspaceAccessRepository.findByWorkspaceAndUser(workspace, existingUser).isPresent()) {
                throw new IllegalArgumentException(email + " already has access to this workspace");
            }
        });
        workspaceInvitationRepository.findByWorkspaceAndEmailIgnoreCaseAndStatus(
                workspace, email, InvitationStatus.PENDING).ifPresent(existing -> {
            throw new IllegalArgumentException(
                    "There's already a pending invitation for " + email + " to this workspace");
        });

        String envIdsString = environmentIds != null && !environmentIds.isEmpty()
                ? environmentIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("")
                : null;

        WorkspaceInvitation invitation = WorkspaceInvitation.builder()
                .workspace(workspace)
                .email(email)
                .role(role)
                .environmentIds(envIdsString)
                .token(generateInviteToken())
                .status(InvitationStatus.PENDING)
                .invitedBy(owner)
                .expiresAt(LocalDateTime.now().plusDays(INVITATION_DAYS_VALID))
                .build();
        return workspaceInvitationRepository.save(invitation);
    }

    @Override
    public List<WorkspaceInvitationResponse> getPendingInvitations(Long workspaceId, User owner) {
        Workspace workspace = workspaceRepository.findByIdAndOwner(workspaceId, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
        return workspaceInvitationRepository.findByWorkspaceAndStatus(workspace, InvitationStatus.PENDING).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void revokeInvitation(Long workspaceId, Long invitationId, User owner) {
        Workspace workspace = workspaceRepository.findByIdAndOwner(workspaceId, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
        WorkspaceInvitation invitation = workspaceInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
        if (!invitation.getWorkspace().getId().equals(workspace.getId())) {
            throw new ResourceNotFoundException("Invitation not found");
        }
        invitation.setStatus(InvitationStatus.REVOKED);
        workspaceInvitationRepository.save(invitation);
    }

    @Override
    @Transactional
    public WorkspaceAccessResponse acceptInvitation(String token, User acceptingUser) {
        WorkspaceInvitation invitation = workspaceInvitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("That invitation code isn't valid"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalArgumentException("This invitation is no longer valid (" + invitation.getStatus() + ")");
        }
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            workspaceInvitationRepository.save(invitation);
            throw new IllegalArgumentException("This invitation has expired — ask for a new one");
        }
        if (!invitation.getEmail().equalsIgnoreCase(acceptingUser.getEmail())) {
            throw new AuthException("This invitation was sent to " + invitation.getEmail()
                    + " — log in with that email to accept it.");
        }

        Workspace workspace = invitation.getWorkspace();
        if (workspace.getOwner().getId().equals(acceptingUser.getId())) {
            // Owns it already — nothing to grant, just close out the invite cleanly.
            invitation.setStatus(InvitationStatus.ACCEPTED);
            workspaceInvitationRepository.save(invitation);
            throw new IllegalArgumentException("This is your own workspace — you already have full access.");
        }

        WorkspaceAccess access = workspaceAccessRepository.findByWorkspaceAndUser(workspace, acceptingUser)
                .orElse(WorkspaceAccess.builder().workspace(workspace).user(acceptingUser).build());
        access.setTeam(null); // direct invite — no team involved
        access.setRole(invitation.getRole());
        access.setAllowedEnvironments(new HashSet<>());
        if (invitation.getEnvironmentIds() != null && !invitation.getEnvironmentIds().isBlank()) {
            List<Long> envIds = Arrays.stream(invitation.getEnvironmentIds().split(","))
                    .map(Long::parseLong)
                    .toList();
            WorkspaceAccess finalAccess = access;
            environmentRepository.findAllById(envIds).stream()
                    .filter(env -> env.getWorkspace().getId().equals(workspace.getId()))
                    .forEach(env -> finalAccess.getAllowedEnvironments().add(env));
        }
        access = workspaceAccessRepository.save(access);

        invitation.setStatus(InvitationStatus.ACCEPTED);
        workspaceInvitationRepository.save(invitation);

        return toAccessResponse(access);
    }

    private void sendInvitationEmail(WorkspaceInvitation invitation, Workspace workspace, User inviter) {
        try {
            emailService.send(invitation.getEmail(),
                    "You've been invited to \"" + workspace.getName() + "\" on Thundercall",
                    (inviter.getUsername() != null ? inviter.getUsername() : inviter.getEmail())
                            + " invited you to their workspace \"" + workspace.getName() + "\" on Thundercall as "
                            + invitation.getRole() + ".\n\n"
                            + "To accept: open Thundercall, log in (or create an account with this email — "
                            + invitation.getEmail() + " — first, if you don't have one), then go to "
                            + "Team Spaces > Enter Invite Code and enter this code:\n\n"
                            + invitation.getToken() + "\n\n"
                            + "This invitation expires in " + INVITATION_DAYS_VALID + " days.");
        } catch (Exception e) {
            log.error("Could not send workspace invitation email to {}: {}", invitation.getEmail(), e.getMessage());
            throw new IllegalStateException("The invitation was created, but the email couldn't be sent ("
                    + e.getMessage() + "). Check the mail settings and try inviting again, or share the code "
                    + invitation.getToken() + " with them directly.");
        }
    }

    private String generateInviteToken() {
        byte[] bytes = new byte[18];
        secureRandom.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private WorkspaceInvitationResponse toResponse(WorkspaceInvitation invitation) {
        return new WorkspaceInvitationResponse(
                invitation.getId(), invitation.getWorkspace().getId(), invitation.getWorkspace().getName(),
                invitation.getEmail(), invitation.getRole(), invitation.getStatus(),
                invitation.getInvitedBy().getUsername(), invitation.getExpiresAt(), invitation.getCreatedAt());
    }

    private WorkspaceAccessResponse toAccessResponse(WorkspaceAccess access) {
        List<Long> envIds = access.getAllowedEnvironments().stream().map(Environment::getId).toList();
        List<String> envNames = access.getAllowedEnvironments().stream().map(Environment::getName).toList();
        return new WorkspaceAccessResponse(
                access.getId(),
                access.getWorkspace().getId(), access.getWorkspace().getName(),
                access.getWorkspace().getOwner().getUsername(),
                access.getUser().getId(), access.getUser().getUsername(), access.getUser().getEmail(),
                access.getTeam() != null ? access.getTeam().getId() : null,
                access.getTeam() != null ? access.getTeam().getName() : null,
                access.getRole(), envIds, envNames, access.getGrantedAt());
    }
}
