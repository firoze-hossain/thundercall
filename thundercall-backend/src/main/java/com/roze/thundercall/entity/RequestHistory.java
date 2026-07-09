package com.roze.thundercall.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "request_history")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RequestHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = true)
    private Request request;
    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime timestamp;
    private Integer statusCode;
    private Long duration;
    @Column(columnDefinition = "TEXT")
    private String response;
    @Column(columnDefinition = "TEXT")
    private String responseHeaders;
}
