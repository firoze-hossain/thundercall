package com.roze.thundercall.api.repository;

import com.roze.thundercall.api.entity.Team;
import com.roze.thundercall.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    @Query("select t from Team t join t.members m where m.user = :user")
    List<Team> findAllByMember(@Param("user") User user);

    @Query("select t from Team t join t.members m where t.id = :teamId and m.user = :user")
    Optional<Team> findByIdAndMember(@Param("teamId") Long teamId, @Param("user") User user);
}
