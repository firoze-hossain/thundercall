package com.roze.thundercall.api.dto;

import jakarta.validation.constraints.NotBlank;

public record MockServerRequest(
        @NotBlank String name,
        String description
) {
}