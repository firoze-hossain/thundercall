package com.roze.thundercall.api.repository;

import com.roze.thundercall.api.entity.MonitorRun;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonitorRunRepository extends JpaRepository<MonitorRun, Long> {
    List<MonitorRun> findByMonitorIdOrderByStartedAtDesc(Long monitorId, Pageable pageable);
}