package com.roze.thundercall.api.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(
        @NotBlank String email,
        @NotBlank String code
) {
}
