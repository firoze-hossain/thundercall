package com.roze.thundercall.api.repository;

import com.roze.thundercall.api.entity.MockRoute;
import com.roze.thundercall.api.enums.HttpMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MockRouteRepository extends JpaRepository<MockRoute, Long> {
    List<MockRoute> findByMockServerIdOrderByCreatedAtAsc(Long mockServerId);

    Optional<MockRoute> findByIdAndMockServerId(Long id, Long mockServerId);

    // Used by the PUBLIC runtime endpoint — deliberately NOT owner-scoped,
    // since anyone (or any tool) is meant to be able to hit a live mock
    // URL, not just the person who created it.
    Optional<MockRoute> findFirstByMockServerIdAndMethodAndPath(Long mockServerId, HttpMethod method, String path);
}