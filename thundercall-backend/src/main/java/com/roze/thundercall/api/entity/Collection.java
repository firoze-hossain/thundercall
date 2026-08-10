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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    // Collection-scoped variables (Postman's "Set as variable" > Collection
    // scope) — visible to every request in this collection, one level
    // below Environment/Global in resolution precedence.
    @ElementCollection
    @CollectionTable(name = "collection_variables", joinColumns = @JoinColumn(name = "collection_id"))
    @MapKeyColumn(name = "variable_key")
    @Column(name = "variable_value", columnDefinition = "TEXT")
    @Builder.Default
    private Map<String, String> variables = new HashMap<>();
    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

}