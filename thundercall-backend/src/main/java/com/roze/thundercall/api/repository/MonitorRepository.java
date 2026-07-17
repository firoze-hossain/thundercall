package com.roze.thundercall.api.repository;

import com.roze.thundercall.api.entity.Monitor;
import com.roze.thundercall.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MonitorRepository extends JpaRepository<Monitor, Long> {
    List<Monitor> findByOwnerOrderByCreatedAtDesc(User owner);

    Optional<Monitor> findByIdAndOwner(Long id, User owner);

    // Used once at app startup to rebuild the live schedule for every
    // monitor that should be running — the in-memory ScheduledFuture map
    // doesn't survive a restart, but the DB's "enabled" flag does.
    List<Monitor> findByEnabledTrue();
}