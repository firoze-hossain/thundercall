package com.roze.thundercall.dto;

import jakarta.validation.constraints.NotBlank;

public record CollectionRequest(
        @NotBlank(message = "Collection name is required")
        String name,
        String description
) {
}
