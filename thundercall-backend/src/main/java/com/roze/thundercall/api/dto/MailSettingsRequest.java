package com.roze.thundercall.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MailSettingsRequest(
        @NotBlank String smtpHost,
        @NotNull Integer smtpPort,
        String smtpUsername,
        // Left null/blank on update to KEEP the currently-stored password —
        // the UI never shows the real one back, so "leave blank = unchanged"
        // avoids forcing the owner to re-enter it every time they tweak
        // something else.
        String smtpPassword,
        @NotBlank String fromAddress,
        String fromName,
        boolean useTls,
        boolean useSsl,
        boolean enabled
) {
}
