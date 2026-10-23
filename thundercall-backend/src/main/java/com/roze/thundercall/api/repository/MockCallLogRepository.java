package com.roze.thundercall.api.repository;

import com.roze.thundercall.api.entity.MockCallLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MockCallLogRepository extends JpaRepository<MockCallLog, Long> {
    List<MockCallLog> findByMockServerIdOrderByTimestampDesc(Long mockServerId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM MockCallLog m WHERE m.mockServer.id = :mockServerId")
    void deleteByMockServerId(Long mockServerId);
}
