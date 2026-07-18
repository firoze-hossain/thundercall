package com.roze.thundercall.api.entity;

import com.roze.thundercall.api.enums.InvitationStatus;
import com.roze.thundercall.api.enums.WorkspaceRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** A direct, standalone invite to a workspace by email — no team
 * involved at all, matching Postman's simpler "just invite someone by
 * email" model rather than requiring them to already be on a team with
 * you first. Mirrors TeamInvitation's proven token/expiry/accept
 * pattern closely. */
@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "workspace_invitations")
public class WorkspaceInvitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkspaceRole role;

    // Comma-separated environment IDs the invitee should see once they
    // accept — same opt-in-only rule as WorkspaceAccess.allowedEnvironments,
    // just stored as a plain string here since there's no access row to
    // attach a proper join table to until the invite is actually accepted.
    private String environmentIds;

    @Column(nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by", nullable = false)
    private User invitedBy;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
