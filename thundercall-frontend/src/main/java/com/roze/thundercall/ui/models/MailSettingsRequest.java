package com.roze.thundercall.ui.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MailSettingsRequest {
    private String smtpHost;
    private Integer smtpPort;
    private String smtpUsername;
    private String smtpPassword;
    private String fromAddress;
    private String fromName;
    private boolean useTls;
    private boolean useSsl;
    private boolean enabled;
}
