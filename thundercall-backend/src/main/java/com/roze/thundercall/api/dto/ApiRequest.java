package com.roze.thundercall.api.dto;

import com.roze.thundercall.api.enums.HttpMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApiRequest(
        String name,
        String description,
        @NotNull HttpMethod method,
        @NotBlank String url,
        String headers,
        String body,
        Long collectionId,
        Long folderId
) {
}
