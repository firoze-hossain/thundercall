package com.roze.thundercall.api.dto;

public record RegisterResponse(
        String username,
        String email,
        boolean requiresVerification,
        String message
) {
}
