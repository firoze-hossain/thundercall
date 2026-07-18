package com.roze.thundercall.api.repository;

import com.roze.thundercall.api.entity.Environment;
import com.roze.thundercall.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnvironmentRepository extends JpaRepository<Environment, Long>, JpaSpecificationExecutor<Environment> {
    List<Environment> findByWorkspaceOwner(User user);

    Optional<Environment> findByIdAndWorkspaceOwner(Long id, User user);

    // Used for browsing a workspace shared with the caller — see the
    // matching note in CollectionRepository.
    List<Environment> findByWorkspaceId(Long workspaceId);

    @Query("SELECT e FROM Environment e WHERE e.workspace.owner = :user AND e.name = :name")
    Optional<Environment> findByNameAndWorkspaceOwner(@Param("name") String name, @Param("user") User user);

    // Scoped to one specific workspace rather than "anywhere this user
    // owns" — used once access to that workspace has already been
    // verified via WorkspaceAccessGuard, so this works correctly for a
    // shared workspace too, not just an owned one.
    @Query("SELECT e FROM Environment e WHERE e.workspace.id = :workspaceId AND e.name = :name")
    Optional<Environment> findByNameAndWorkspaceId(@Param("name") String name, @Param("workspaceId") Long workspaceId);

    List<Environment> findByWorkspaceOwnerAndIsActive(User user, Boolean isActive);
}