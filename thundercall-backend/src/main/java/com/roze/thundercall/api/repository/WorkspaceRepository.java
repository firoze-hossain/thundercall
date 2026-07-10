package com.roze.thundercall.api.repository;

import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long>, JpaSpecificationExecutor<Workspace> {
    @Query("select  count (w) > 0 from Workspace w where  w.owner=:user")
    boolean existsByOwner(@Param("user") User user);

    List<Workspace> findByOwner(User owner);

    Optional<Workspace> findByIdAndOwner(Long id, User owner);
}
