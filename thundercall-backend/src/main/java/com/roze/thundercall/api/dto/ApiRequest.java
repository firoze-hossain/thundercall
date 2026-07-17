package com.roze.thundercall.api.dto;

import com.roze.thundercall.api.enums.HttpMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ApiRequest(
        String name,
        String description,
        @NotNull HttpMethod method,
        @NotBlank String url,
        String headers,
        String body,
        Long collectionId,
        Long folderId,
        String preRequestScript,
        String testsScript,
        String authType,
        String authToken,
        String authUsername,
        String authPassword,
        // Present only for the "form-data" body type — when set, this is
        // used to build a real multipart/form-data request (actual file
        // bytes and all) instead of the flat "body" string.
        List<FormDataField> formData
) {
}