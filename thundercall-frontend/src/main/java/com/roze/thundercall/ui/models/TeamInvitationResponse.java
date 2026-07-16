package com.roze.thundercall.ui.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamInvitationResponse {
    private Long id;
    private Long teamId;
    private String teamName;
    private String email;
    private String role;
    private String status;
    private String invitedByUsername;
    private String expiresAt;
    private String createdAt;
}