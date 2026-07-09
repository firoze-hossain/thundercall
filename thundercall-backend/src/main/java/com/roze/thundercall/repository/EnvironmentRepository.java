package com.roze.thundercall.repository;

import com.roze.thundercall.entity.Environment;
import com.roze.thundercall.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnvironmentRepository extends JpaRepository<Environment, Long>, JpaSpecificationExecutor<Environment> {
    List<Environment> findByWorkspaceOwner(User user);

    Optional<Environment> findByIdAndWorkspaceOwner(Long id, User user);

    @Query("SELECT e FROM Environment e WHERE e.workspace.owner = :user AND e.name = :name")
    Optional<Environment> findByNameAndWorkspaceOwner(@Param("name") String name, @Param("user") User user);

    List<Environment> findByWorkspaceOwnerAndIsActive(User user, Boolean isActive);
}