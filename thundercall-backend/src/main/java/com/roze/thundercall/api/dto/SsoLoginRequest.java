package com.roze.thundercall.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SsoLoginRequest(
        @NotBlank @Email
        String email,
        String username,
        String token,
        String provider
) {
}
