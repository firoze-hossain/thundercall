package com.roze.thundercall.api.repository;

import com.roze.thundercall.api.entity.Team;
import com.roze.thundercall.api.entity.TeamInvitation;
import com.roze.thundercall.api.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, Long> {
    Optional<TeamInvitation> findByToken(String token);

    List<TeamInvitation> findByTeamAndStatus(Team team, InvitationStatus status);

    Optional<TeamInvitation> findByTeamAndEmailIgnoreCaseAndStatus(Team team, String email, InvitationStatus status);
}
