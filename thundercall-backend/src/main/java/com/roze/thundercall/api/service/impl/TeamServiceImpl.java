package com.roze.thundercall.api.service.impl;

import com.roze.thundercall.api.dto.*;
import com.roze.thundercall.api.entity.Team;
import com.roze.thundercall.api.entity.TeamInvitation;
import com.roze.thundercall.api.entity.TeamMember;
import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.enums.InvitationStatus;
import com.roze.thundercall.api.enums.TeamRole;
import com.roze.thundercall.api.exception.AuthException;
import com.roze.thundercall.api.exception.ResourceNotFoundException;
import com.roze.thundercall.api.repository.TeamInvitationRepository;
import com.roze.thundercall.api.repository.TeamMemberRepository;
import com.roze.thundercall.api.repository.TeamRepository;
import com.roze.thundercall.api.repository.UserRepository;
import com.roze.thundercall.api.service.EmailService;
import com.roze.thundercall.api.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Core team management: create a team, invite members by email (a token
 * they redeem inside the app — no web server here to host a click-through
 * link, so the invite email includes the token to paste into
 * Teams > Join Team), accept/revoke invitations, roles, removal.
 *
 * Three roles per team: OWNER (created it, full control, can't be
 * removed or demoted), ADMIN (can invite/remove ordinary members), MEMBER
 * (regular participant). Kept intentionally simpler than Postman's full
 * model — no ownership transfer or per-resource sharing yet; those are
 * natural next steps on top of this.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TeamServiceImpl implements TeamService {
    private static final int INVITATION_DAYS_VALID = 7;

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamInvitationRepository teamInvitationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public TeamResponse createTeam(TeamRequest request, User owner) {
        Team team = Team.builder()
                .name(request.name())
                .description(request.description())
                .owner(owner)
                .build();
        team = teamRepository.save(team);

        TeamMember ownerMembership = TeamMember.builder()
                .team(team)
                .user(owner)
                .role(TeamRole.OWNER)
                .build();
        teamMemberRepository.save(ownerMembership);

        return toTeamResponse(team, TeamRole.OWNER);
    }

    @Override
    public List<TeamResponse> getMyTeams(User user) {
        return teamRepository.findAllByMember(user).stream()
                .map(team -> toTeamResponse(team, roleOf(team, user)))
                .toList();
    }

    @Override
    public TeamResponse getTeam(Long teamId, User user) {
        Team team = requireMembership(teamId, user);
        return toTeamResponse(team, roleOf(team, user));
    }

    @Override
    public List<TeamMemberResponse> getMembers(Long teamId, User user) {
        Team team = requireMembership(teamId, user);
        return teamMemberRepository.findByTeam(team).stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Override
    @Transactional
    public TeamInvitationResponse inviteMember(Long teamId, InviteMemberRequest request, User inviter) {
        Team team = requireAdminOrOwner(teamId, inviter);

        userRepository.findByEmailIgnoreCase(request.email()).ifPresent(existingUser -> {
            if (teamMemberRepository.existsByTeamAndUser(team, existingUser)) {
                throw new IllegalArgumentException(request.email() + " is already a member of this team");
            }
        });
        teamInvitationRepository.findByTeamAndEmailIgnoreCaseAndStatus(team, request.email(), InvitationStatus.PENDING)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "There's already a pending invitation for " + request.email() + " on this team");
                });

        String token = generateInviteToken();
        TeamInvitation invitation = TeamInvitation.builder()
                .team(team)
                .email(request.email())
                .role(request.role())
                .token(token)
                .status(InvitationStatus.PENDING)
                .invitedBy(inviter)
                .expiresAt(LocalDateTime.now().plusDays(INVITATION_DAYS_VALID))
                .build();
        invitation = teamInvitationRepository.save(invitation);

        sendInvitationEmail(invitation, team, inviter);

        return toInvitationResponse(invitation);
    }

    @Override
    public List<TeamInvitationResponse> getPendingInvitations(Long teamId, User user) {
        Team team = requireAdminOrOwner(teamId, user);
        return teamInvitationRepository.findByTeamAndStatus(team, InvitationStatus.PENDING).stream()
                .map(this::toInvitationResponse)
                .toList();
    }

    @Override
    @Transactional
    public void revokeInvitation(Long teamId, Long invitationId, User user) {
        Team team = requireAdminOrOwner(teamId, user);
        TeamInvitation invitation = teamInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
        if (!invitation.getTeam().getId().equals(team.getId())) {
            throw new ResourceNotFoundException("Invitation not found");
        }
        invitation.setStatus(InvitationStatus.REVOKED);
        teamInvitationRepository.save(invitation);
    }

    @Override
    @Transactional
    public TeamResponse acceptInvitation(String token, User acceptingUser) {
        TeamInvitation invitation = teamInvitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("That invitation code isn't valid"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalArgumentException("This invitation is no longer valid (" + invitation.getStatus() + ")");
        }
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            teamInvitationRepository.save(invitation);
            throw new IllegalArgumentException("This invitation has expired — ask for a new one");
        }
        if (!invitation.getEmail().equalsIgnoreCase(acceptingUser.getEmail())) {
            throw new AuthException("This invitation was sent to " + invitation.getEmail()
                    + " — log in with that email to accept it.");
        }

        Team team = invitation.getTeam();
        if (teamMemberRepository.existsByTeamAndUser(team, acceptingUser)) {
            invitation.setStatus(InvitationStatus.ACCEPTED);
            teamInvitationRepository.save(invitation);
            return toTeamResponse(team, roleOf(team, acceptingUser));
        }

        TeamMember member = TeamMember.builder()
                .team(team)
                .user(acceptingUser)
                .role(invitation.getRole())
                .build();
        teamMemberRepository.save(member);

        invitation.setStatus(InvitationStatus.ACCEPTED);
        teamInvitationRepository.save(invitation);

        return toTeamResponse(team, invitation.getRole());
    }

    @Override
    @Transactional
    public void removeMember(Long teamId, Long memberUserId, User actingUser) {
        Team team = requireAdminOrOwner(teamId, actingUser);
        User targetUser = userRepository.findById(memberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        TeamMember target = teamMemberRepository.findByTeamAndUser(team, targetUser)
                .orElseThrow(() -> new ResourceNotFoundException("That user isn't a member of this team"));

        if (target.getRole() == TeamRole.OWNER) {
            throw new IllegalArgumentException("The team owner can't be removed — delete the team instead");
        }
        TeamRole actingRole = roleOf(team, actingUser);
        if (actingRole == TeamRole.ADMIN && target.getRole() == TeamRole.ADMIN) {
            throw new AuthException("Only the team owner can remove another admin");
        }

        teamMemberRepository.delete(target);
    }

    @Override
    @Transactional
    public void changeMemberRole(Long teamId, Long memberUserId, TeamRole newRole, User actingUser) {
        Team team = requireOwner(teamId, actingUser);
        User targetUser = userRepository.findById(memberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        TeamMember target = teamMemberRepository.findByTeamAndUser(team, targetUser)
                .orElseThrow(() -> new ResourceNotFoundException("That user isn't a member of this team"));

        if (target.getRole() == TeamRole.OWNER) {
            throw new IllegalArgumentException("The owner's role can't be changed this way");
        }
        if (newRole == TeamRole.OWNER) {
            throw new IllegalArgumentException("Transferring ownership isn't supported yet");
        }
        target.setRole(newRole);
        teamMemberRepository.save(target);
    }

    @Override
    @Transactional
    public void leaveTeam(Long teamId, User user) {
        Team team = requireMembership(teamId, user);
        TeamMember membership = teamMemberRepository.findByTeamAndUser(team, user)
                .orElseThrow(() -> new ResourceNotFoundException("You're not a member of this team"));
        if (membership.getRole() == TeamRole.OWNER) {
            throw new IllegalArgumentException(
                    "As the owner, you can't leave — delete the team instead (or transfer ownership, coming soon)");
        }
        teamMemberRepository.delete(membership);
    }

    @Override
    @Transactional
    public void deleteTeam(Long teamId, User user) {
        Team team = requireOwner(teamId, user);
        teamRepository.delete(team);
    }

    // ---------- helpers ----------

    private Team requireMembership(Long teamId, User user) {
        return teamRepository.findByIdAndMember(teamId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
    }

    private Team requireAdminOrOwner(Long teamId, User user) {
        Team team = requireMembership(teamId, user);
        TeamRole role = roleOf(team, user);
        if (role != TeamRole.OWNER && role != TeamRole.ADMIN) {
            throw new AuthException("Only a team owner or admin can do that");
        }
        return team;
    }

    private Team requireOwner(Long teamId, User user) {
        Team team = requireMembership(teamId, user);
        if (roleOf(team, user) != TeamRole.OWNER) {
            throw new AuthException("Only the team owner can do that");
        }
        return team;
    }

    private TeamRole roleOf(Team team, User user) {
        return teamMemberRepository.findByTeamAndUser(team, user)
                .map(TeamMember::getRole)
                .orElse(null);
    }

    private String generateInviteToken() {
        // URL/email/copy-paste friendly: hex, no ambiguous characters.
        byte[] bytes = new byte[18];
        secureRandom.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void sendInvitationEmail(TeamInvitation invitation, Team team, User inviter) {
        try {
            emailService.send(invitation.getEmail(), "You've been invited to join \"" + team.getName() + "\" on Thundercall",
                    (inviter.getUsername() != null ? inviter.getUsername() : inviter.getEmail())
                            + " invited you to join the \"" + team.getName() + "\" team on Thundercall as "
                            + invitation.getRole() + ".\n\n"
                            + "To accept: open Thundercall, log in (or create an account with this email — "
                            + invitation.getEmail() + " — first, if you don't have one), then go to "
                            + "Teams > Join Team and enter this code:\n\n"
                            + invitation.getToken() + "\n\n"
                            + "This invitation expires in " + INVITATION_DAYS_VALID + " days.");
        } catch (Exception e) {
            log.error("Could not send team invitation email to {}: {}", invitation.getEmail(), e.getMessage());
            throw new IllegalStateException("The invitation was created, but the email couldn't be sent ("
                    + e.getMessage() + "). Check the mail settings and try inviting again, or share the code "
                    + invitation.getToken() + " with them directly.");
        }
    }

    private TeamResponse toTeamResponse(Team team, TeamRole myRole) {
        long memberCount = teamMemberRepository.countByTeam(team);
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getDescription(),
                team.getOwner().getUsername(),
                team.getOwner().getEmail(),
                memberCount,
                myRole,
                team.getCreatedAt()
        );
    }

    private TeamMemberResponse toMemberResponse(TeamMember member) {
        return new TeamMemberResponse(
                member.getId(),
                member.getUser().getId(),
                member.getUser().getUsername(),
                member.getUser().getEmail(),
                member.getRole(),
                member.getJoinedAt()
        );
    }

    private TeamInvitationResponse toInvitationResponse(TeamInvitation invitation) {
        return new TeamInvitationResponse(
                invitation.getId(),
                invitation.getTeam().getId(),
                invitation.getTeam().getName(),
                invitation.getEmail(),
                invitation.getRole(),
                invitation.getStatus(),
                invitation.getInvitedBy().getUsername(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt()
        );
    }
}
