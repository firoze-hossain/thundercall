package com.roze.thundercall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FolderRequest(
    @NotBlank(message = "Folder name is required")
    String name,
    String description,
    @NotNull(message = "Collection ID is required")
    Long collectionId
) {}