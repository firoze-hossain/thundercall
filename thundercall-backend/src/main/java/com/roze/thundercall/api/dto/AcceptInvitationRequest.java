package com.roze.thundercall.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AcceptInvitationRequest(
        @NotBlank String token
) {
}
