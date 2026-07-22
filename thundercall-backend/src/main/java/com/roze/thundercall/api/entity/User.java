package com.roze.thundercall.api.entity;

import com.roze.thundercall.api.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    private Role role;

    // ---- Profile fields (editable by the user in the Profile dialog) ----
    @Column(name = "full_name", length = 100)
    private String fullName;

    /** Base64-encoded avatar image (PNG/JPEG). Stored as TEXT — avatars
     * are resized client-side to a small square before upload, so the
     * payload stays tiny. Null means "no avatar set". */
    @Column(columnDefinition = "TEXT")
    private String avatar;

    private Boolean enabled;

    // Email verification (separate from "enabled", which is for admin
    // disable/enable — a user can be enabled but not yet verified).
    // NOTE: default value is set explicitly in UserMapper.toEntity(),
    // not here — this class is constructed via "new User()" + setters
    // in most places, not the Lombok builder, so a builder-only default
    // wouldn't reliably apply.
    private Boolean emailVerified;
    private String verificationCode;
    private LocalDateTime verificationCodeExpiresAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
