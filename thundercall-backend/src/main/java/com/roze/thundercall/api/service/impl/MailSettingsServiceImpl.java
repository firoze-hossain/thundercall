package com.roze.thundercall.api.service.impl;

import com.roze.thundercall.api.dto.MailSettingsRequest;
import com.roze.thundercall.api.dto.MailSettingsResponse;
import com.roze.thundercall.api.entity.MailSettings;
import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.enums.Role;
import com.roze.thundercall.api.exception.AuthException;
import com.roze.thundercall.api.repository.MailSettingsRepository;
import com.roze.thundercall.api.service.EmailService;
import com.roze.thundercall.api.service.MailSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MailSettingsServiceImpl implements MailSettingsService {
    private final MailSettingsRepository mailSettingsRepository;
    private final EmailService emailService;

    @Override
    public MailSettingsResponse getSettings(User requestingUser) {
        requireAdmin(requestingUser);
        return mailSettingsRepository.findAll().stream()
                .findFirst()
                .map(this::toResponse)
                // FIX: this placeholder used to default "enabled" to false,
                // so a brand-new setup silently stayed off even after
                // someone filled in real SMTP details and hit Save — with
                // no strong visual cue that a separate switch also needed
                // flipping. Defaulting to true here means filling in valid
                // details and saving just works, as expected.
                .orElseGet(() -> new MailSettingsResponse(
                        null, "", 587, "", false, "", "", true, false, true, null));
    }

    @Override
    @Transactional
    public MailSettingsResponse updateSettings(MailSettingsRequest request, User requestingUser) {
        requireAdmin(requestingUser);

        // Only one row is ever expected — reuse it if present, otherwise
        // this is the first time anyone has configured mail.
        MailSettings settings = mailSettingsRepository.findAll().stream()
                .findFirst()
                .orElseGet(MailSettings::new);

        settings.setSmtpHost(request.smtpHost());
        settings.setSmtpPort(request.smtpPort());
        settings.setSmtpUsername(request.smtpUsername());
        // Blank password on update means "keep the existing one" — the UI
        // never shows the real password back, so there'd be nothing
        // sensible to re-submit unless the owner is actually changing it.
        if (request.smtpPassword() != null && !request.smtpPassword().isBlank()) {
            settings.setSmtpPassword(request.smtpPassword());
        }
        settings.setFromAddress(request.fromAddress());
        settings.setFromName(request.fromName());
        settings.setUseTls(request.useTls());
        settings.setUseSsl(request.useSsl());
        settings.setEnabled(request.enabled());

        settings = mailSettingsRepository.save(settings);
        return toResponse(settings);
    }

    @Override
    public void sendTestEmail(String toAddress, User requestingUser) {
        requireAdmin(requestingUser);
        emailService.send(toAddress, "Thundercall test email",
                "This is a test email from your Thundercall server's mail settings.\n\n"
                        + "If you're reading this, outgoing mail is configured correctly.");
    }

    private void requireAdmin(User user) {
        if (user.getRole() != Role.ADMIN) {
            throw new AuthException("Only an admin can view or change mail settings.");
        }
    }

    private MailSettingsResponse toResponse(MailSettings settings) {
        return new MailSettingsResponse(
                settings.getId(),
                settings.getSmtpHost(),
                settings.getSmtpPort(),
                settings.getSmtpUsername(),
                settings.getSmtpPassword() != null && !settings.getSmtpPassword().isBlank(),
                settings.getFromAddress(),
                settings.getFromName(),
                settings.isUseTls(),
                settings.isUseSsl(),
                settings.isEnabled(),
                settings.getUpdatedAt()
        );
    }
}