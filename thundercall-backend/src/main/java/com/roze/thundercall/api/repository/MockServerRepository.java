package com.roze.thundercall.api.repository;

import com.roze.thundercall.api.entity.MockServer;
import com.roze.thundercall.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MockServerRepository extends JpaRepository<MockServer, Long> {
    List<MockServer> findByOwnerOrderByCreatedAtDesc(User owner);

    Optional<MockServer> findByIdAndOwner(Long id, User owner);
}