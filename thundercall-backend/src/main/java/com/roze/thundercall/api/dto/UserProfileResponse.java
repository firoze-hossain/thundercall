package com.roze.thundercall.api.dto;

import com.roze.thundercall.api.enums.Role;

public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String avatarBase64,
        Role role
) {
}
