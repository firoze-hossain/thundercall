package com.roze.thundercall.dto;

import com.roze.thundercall.enums.HttpMethod;
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
