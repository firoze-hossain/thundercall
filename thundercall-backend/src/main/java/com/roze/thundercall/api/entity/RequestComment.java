package com.roze.thundercall.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A Postman-style inline comment thread anchored to a text range within a
 * request's body/scripts (right-click "Comment" on selected text). The
 * root comment of a thread has {@code parent == null} and carries the
 * anchor (fieldName/rangeStart/rangeEnd/snippet) plus the resolved flag;
 * replies are child rows pointing back at the thread's root via
 * {@code parent}, mirroring the "Add a reply" box under a thread in
 * Postman's UI.
 */
@Entity
@Table(name = "request_comments")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RequestComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private Request request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private RequestComment parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RequestComment> replies = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /** Which editor the thread is anchored to — "body", "preRequestScript",
     * "testsScript" — since a request has more than one commentable field.
     * Only meaningful on the root comment. */
    @Column(name = "field_name")
    private String fieldName;

    /** Character offsets into that field's text at the moment the thread
     * was created. Only meaningful on the root comment. */
    private Integer rangeStart;
    private Integer rangeEnd;

    /** A short preview of the selected text, captured at creation time for
     * the "Body > raw > {snippet}" breadcrumb — kept even if the body text
     * later changes underneath it. Only meaningful on the root comment. */
    @Column(columnDefinition = "TEXT")
    private String snippet;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /** Only meaningful on the root comment; replies are never individually
     * "resolved". */
    @Column(nullable = false)
    @Builder.Default
    private Boolean resolved = false;

    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
