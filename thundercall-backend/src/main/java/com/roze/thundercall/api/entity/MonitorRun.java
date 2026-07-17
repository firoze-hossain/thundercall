package com.roze.thundercall.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "monitor_runs")
public class MonitorRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitor_id", nullable = false)
    private Monitor monitor;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private int totalRequests;
    private int passedRequests;
    private int failedRequests;
    private long avgResponseTimeMs;

    @Column(nullable = false)
    private boolean success;

    // JSON array — one entry per request: name, method, url, statusCode,
    // durationMs, success, error. Kept as a single TEXT column same as
    // headers/body elsewhere in this app, rather than a child table —
    // it's only ever read back whole, never queried per-field.
    @Column(columnDefinition = "TEXT")
    private String details;
}