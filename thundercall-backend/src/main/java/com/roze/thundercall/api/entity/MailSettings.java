package com.roze.thundercall.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Holds the app's own outgoing-mail (SMTP) configuration. Deliberately a
 * normal DB row rather than static application.yml properties — the
 * project owner can view/change it from within the app (Settings screen)
 * without a server restart, since EmailServiceImpl builds a fresh
 * JavaMailSenderImpl from whatever is currently in this table on every
 * send rather than using a fixed Spring-managed bean.
 *
 * Only one row is ever expected to exist (id=1); MailSettingsServiceImpl
 * enforces that.
 */
@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "mail_settings")
public class MailSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String smtpHost;

    @Column(nullable = false)
    private Integer smtpPort;

    private String smtpUsername;

    // NOTE: stored as-is (not hashed, since we need the plaintext to
    // authenticate with the SMTP server) — never returned in API
    // responses, see MailSettingsResponse which omits it entirely.
    private String smtpPassword;

    @Column(nullable = false)
    private String fromAddress;

    private String fromName;

    @Column(nullable = false)
    private boolean useTls;

    @Column(nullable = false)
    private boolean useSsl;

    @Column(nullable = false)
    private boolean enabled;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
