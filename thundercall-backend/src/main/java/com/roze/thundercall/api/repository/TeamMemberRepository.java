package com.roze.thundercall.api.repository;

import com.roze.thundercall.api.entity.Team;
import com.roze.thundercall.api.entity.TeamMember;
import com.roze.thundercall.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findByTeam(Team team);

    // All of a user's own team memberships — powers presence: "who are my
    // teammates" so the online list only ever shows people related to you.
    List<TeamMember> findByUser(User user);

    Optional<TeamMember> findByTeamAndUser(Team team, User user);

    boolean existsByTeamAndUser(Team team, User user);

    long countByTeam(Team team);
}
