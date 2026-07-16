package com.roze.thundercall.api.dto;

import com.roze.thundercall.api.enums.TeamRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteMemberRequest(
        @NotBlank String email,
        @NotNull TeamRole role
) {
}
