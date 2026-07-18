package com.roze.thundercall.api.repository;

import com.roze.thundercall.api.entity.Workspace;
import com.roze.thundercall.api.entity.WorkspaceInvitation;
import com.roze.thundercall.api.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, Long> {
    Optional<WorkspaceInvitation> findByToken(String token);

    List<WorkspaceInvitation> findByWorkspaceAndStatus(Workspace workspace, InvitationStatus status);

    Optional<WorkspaceInvitation> findByWorkspaceAndEmailIgnoreCaseAndStatus(
            Workspace workspace, String email, InvitationStatus status);
}
