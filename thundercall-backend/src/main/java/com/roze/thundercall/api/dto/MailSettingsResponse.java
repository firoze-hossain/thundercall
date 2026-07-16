package com.roze.thundercall.api.dto;

import java.time.LocalDateTime;

public record MailSettingsResponse(
        Long id,
        String smtpHost,
        Integer smtpPort,
        String smtpUsername,
        boolean hasPassword,
        String fromAddress,
        String fromName,
        boolean useTls,
        boolean useSsl,
        boolean enabled,
        LocalDateTime updatedAt
) {
}
