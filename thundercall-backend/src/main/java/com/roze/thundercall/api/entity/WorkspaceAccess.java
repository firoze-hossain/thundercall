package com.roze.thundercall.api.entity;

import com.roze.thundercall.api.enums.WorkspaceRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/** One row = one team member's access to one workspace they don't own.
 * The owner never needs a row here — they always have full access to
 * their own workspace regardless of this table. Sharing happens "in
 * the context of" a team (you can only share with people who are
 * actually on a team with you), but access itself is per-user, not
 * per-team — matching Postman's real model where two members of the
 * same team can have completely different roles on the same
 * workspace. */
@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "workspace_access", uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "user_id"}))
public class WorkspaceAccess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Which team this share happened under, if any — kept for display
    // ("shared via Engineering Team") when sharing did go through a
    // team. Nullable: direct email invites (WorkspaceInvitation) don't
    // involve a team at all, matching Postman's own simpler model where
    // you can just invite someone to a workspace by email directly.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkspaceRole role;

    // Explicit opt-in allow-list — empty by default, meaning a member
    // with workspace access sees NO environments until the owner
    // specifically grants some. Collections/folders/requests aren't
    // gated this way; only environments, since they're where secrets
    // (API keys, tokens, prod URLs) actually live.
    @ManyToMany
    @JoinTable(
            name = "workspace_access_environments",
            joinColumns = @JoinColumn(name = "workspace_access_id"),
            inverseJoinColumns = @JoinColumn(name = "environment_id"))
    @Builder.Default
    private Set<Environment> allowedEnvironments = new HashSet<>();

    @CreationTimestamp
    private LocalDateTime grantedAt;
}
