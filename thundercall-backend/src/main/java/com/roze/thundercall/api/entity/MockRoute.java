package com.roze.thundercall.api.entity;

import com.roze.thundercall.api.enums.HttpMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "mock_routes")
public class MockRoute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mock_server_id", nullable = false)
    private MockServer mockServer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HttpMethod method;

    // Matched exactly against the incoming request's path, e.g. "/users/1".
    // Wildcard/param matching ("/users/:id") is a reasonable next step but
    // out of scope for this pass — see the README note.
    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private int responseStatus;

    @Column(columnDefinition = "TEXT")
    private String responseBody;

    // JSON string, e.g. {"Content-Type":"application/json"} — kept as a
    // single string same as everywhere else in this app stores headers.
    @Column(columnDefinition = "TEXT")
    private String responseHeaders;

    @Builder.Default
    private int delayMs = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;
}