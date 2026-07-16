package com.roze.thundercall.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ResendVerificationRequest(
        @NotBlank String email
) {
}
