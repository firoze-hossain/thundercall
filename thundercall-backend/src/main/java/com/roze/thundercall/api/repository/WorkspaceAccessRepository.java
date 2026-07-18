package com.roze.thundercall.api.repository;

import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.entity.Workspace;
import com.roze.thundercall.api.entity.WorkspaceAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkspaceAccessRepository extends JpaRepository<WorkspaceAccess, Long> {
    // Workspaces shared WITH a given user — powers their "Team Workspaces" list.
    List<WorkspaceAccess> findByUserOrderByGrantedAtDesc(User user);

    // Who has access to a given workspace — powers the owner's sharing/management UI.
    List<WorkspaceAccess> findByWorkspaceOrderByGrantedAtDesc(Workspace workspace);

    Optional<WorkspaceAccess> findByWorkspaceAndUser(Workspace workspace, User user);

    void deleteByWorkspaceAndUser(Workspace workspace, User user);
}
