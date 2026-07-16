package com.roze.thundercall.api.service;

import com.roze.thundercall.api.dto.MailSettingsRequest;
import com.roze.thundercall.api.dto.MailSettingsResponse;
import com.roze.thundercall.api.entity.User;

public interface MailSettingsService {
    MailSettingsResponse getSettings(User requestingUser);

    MailSettingsResponse updateSettings(MailSettingsRequest request, User requestingUser);

    void sendTestEmail(String toAddress, User requestingUser);
}
