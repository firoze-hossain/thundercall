package com.roze.thundercall.api.service.impl;

import com.roze.thundercall.api.dto.OnlineUserResponse;
import com.roze.thundercall.api.entity.TeamMember;
import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.entity.Workspace;
import com.roze.thundercall.api.entity.WorkspaceAccess;
import com.roze.thundercall.api.repository.TeamMemberRepository;
import com.roze.thundercall.api.repository.WorkspaceAccessRepository;
import com.roze.thundercall.api.repository.WorkspaceRepository;
import com.roze.thundercall.api.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory presence tracking. The desktop client POSTs a heartbeat every
 * ~25 seconds; a user is considered online if their last heartbeat is
 * within ONLINE_WINDOW. No database table needed — presence is inherently
 * ephemeral, and losing it on restart just means everyone shows offline
 * for one heartbeat cycle.
 */
@Service
@RequiredArgsConstructor
public class PresenceServiceImpl implements PresenceService {
    private static final Duration ONLINE_WINDOW = Duration.ofSeconds(70);

    private final TeamMemberRepository teamMemberRepository;
    private final WorkspaceAccessRepository workspaceAccessRepository;
    private final WorkspaceRepository workspaceRepository;

    private final Map<Long, Instant> lastSeen = new ConcurrentHashMap<>();

    @Override
    public void heartbeat(User user) {
        lastSeen.put(user.getId(), Instant.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OnlineUserResponse> getOnlineRelatedUsers(User viewer) {
        Instant cutoff = Instant.now().minus(ONLINE_WINDOW);
        // LinkedHashMap keyed by user id dedupes people reachable through
        // several relationships (same team AND a shared workspace).
        Map<Long, User> related = new LinkedHashMap<>();

        // 1) Teammates — everyone in any team the viewer belongs to.
        for (TeamMember myMembership : teamMemberRepository.findByUser(viewer)) {
            for (TeamMember member : teamMemberRepository.findByTeam(myMembership.getTeam())) {
                related.putIfAbsent(member.getUser().getId(), member.getUser());
            }
        }

        // 2) Workspaces shared WITH the viewer — the owner plus everyone
        //    else who has access to those same workspaces.
        for (WorkspaceAccess access : workspaceAccessRepository.findByUserOrderByGrantedAtDesc(viewer)) {
            Workspace workspace = access.getWorkspace();
            related.putIfAbsent(workspace.getOwner().getId(), workspace.getOwner());
            for (WorkspaceAccess peer : workspaceAccessRepository.findByWorkspaceOrderByGrantedAtDesc(workspace)) {
                related.putIfAbsent(peer.getUser().getId(), peer.getUser());
            }
        }

        // 3) Workspaces the viewer OWNS — everyone they granted access to.
        for (Workspace owned : workspaceRepository.findByOwner(viewer)) {
            for (WorkspaceAccess grant : workspaceAccessRepository.findByWorkspaceOrderByGrantedAtDesc(owned)) {
                related.putIfAbsent(grant.getUser().getId(), grant.getUser());
            }
        }

        related.remove(viewer.getId());

        List<OnlineUserResponse> online = new ArrayList<>();
        for (User user : related.values()) {
            Instant seen = lastSeen.get(user.getId());
            if (seen != null && seen.isAfter(cutoff)) {
                online.add(new OnlineUserResponse(user.getId(), user.getUsername(), user.getFullName()));
            }
        }
        online.sort(Comparator.comparing(OnlineUserResponse::username, String.CASE_INSENSITIVE_ORDER));
        return online;
    }

    /** Housekeeping so the map can't grow forever on a long-lived server:
     * entries older than 10 minutes are useless and get dropped. */
    @Scheduled(fixedDelay = 600_000)
    public void evictStaleEntries() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(10));
        lastSeen.entrySet().removeIf(e -> e.getValue().isBefore(cutoff));
    }
}
