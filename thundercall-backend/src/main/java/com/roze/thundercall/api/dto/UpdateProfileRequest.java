package com.roze.thundercall.api.dto;

import jakarta.validation.constraints.Size;

/** Both fields are optional; a null avatarBase64 combined with
 * removeAvatar=false means "keep whatever avatar is already stored",
 * while removeAvatar=true clears it. The size cap keeps a resized
 * ~256px avatar comfortably inside the limit while rejecting people
 * trying to upload raw camera photos as base64. */
public record UpdateProfileRequest(
        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,
        @Size(max = 400_000, message = "Avatar image is too large — please choose a smaller image")
        String avatarBase64,
        boolean removeAvatar
) {
}
