package com.roze.thundercall.api.service;

import com.roze.thundercall.api.dto.*;
import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.enums.TeamRole;

import java.util.List;

public interface TeamService {
    TeamResponse createTeam(TeamRequest request, User owner);

    List<TeamResponse> getMyTeams(User user);

    TeamResponse getTeam(Long teamId, User user);

    List<TeamMemberResponse> getMembers(Long teamId, User user);

    TeamInvitationResponse inviteMember(Long teamId, InviteMemberRequest request, User inviter);

    List<TeamInvitationResponse> getPendingInvitations(Long teamId, User user);

    void revokeInvitation(Long teamId, Long invitationId, User user);

    TeamResponse acceptInvitation(String token, User acceptingUser);

    void removeMember(Long teamId, Long memberUserId, User actingUser);

    void changeMemberRole(Long teamId, Long memberUserId, TeamRole newRole, User actingUser);

    void leaveTeam(Long teamId, User user);

    void deleteTeam(Long teamId, User user);
}
