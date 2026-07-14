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

@Entity
@Table(name = "collections")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Collection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    private String description;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id",nullable = false)
    private Workspace workspace;
    @OneToMany(mappedBy = "collection",cascade = CascadeType.ALL,orphanRemoval = true)
    private List<Request>requests=new ArrayList<>();
    // FIX: deleting a collection with folders in it failed with a foreign
    // key violation — Collection cascaded its requests but had NO mapping
    // at all for its folders, so Hibernate had no way to know they needed
    // deleting first. Folder's own self-referencing cascade (added
    // earlier) then takes care of nested sub-folders automatically.
    @OneToMany(mappedBy = "collection",cascade = CascadeType.ALL,orphanRemoval = true)
    @Builder.Default
    private List<Folder> folders = new ArrayList<>();
    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

}