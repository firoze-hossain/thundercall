package com.roze.thundercall.ui.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.roze.thundercall.ui.models.BaseResponse;
import com.roze.thundercall.ui.models.MailSettingsRequest;
import com.roze.thundercall.ui.models.MailSettingsResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class MailSettingsService {
    private static final String BASE_URL = "/mail-settings";

    public static Optional<MailSettingsResponse> getSettings() throws IOException {
        BaseResponse<MailSettingsResponse> response = ApiClient.get(BASE_URL,
                new TypeReference<BaseResponse<MailSettingsResponse>>() {
                });
        return response != null ? Optional.ofNullable(response.getData()) : Optional.empty();
    }

    public static MailSettingsResponse updateSettings(MailSettingsRequest request) throws IOException {
        BaseResponse<MailSettingsResponse> response = ApiClient.put(BASE_URL, request,
                new TypeReference<BaseResponse<MailSettingsResponse>>() {
                });
        if (response == null || response.getData() == null) {
            throw new IOException("Invalid response from server");
        }
        return response.getData();
    }

    public static void sendTestEmail(String toAddress) throws IOException {
        String encoded = URLEncoder.encode(toAddress, StandardCharsets.UTF_8);
        ApiClient.post(BASE_URL + "/test?toAddress=" + encoded, null,
                new TypeReference<BaseResponse<Void>>() {
                });
    }
}
