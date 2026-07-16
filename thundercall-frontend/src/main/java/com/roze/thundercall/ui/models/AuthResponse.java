package com.roze.thundercall.ui.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private Long id;
    private String token;
    private String refreshToken;
    private String username;
    private String email;
    // FIX: this was @JsonIgnore'd, so the frontend never actually knew
    // the logged-in user's role — meaning there was no way to hide
    // admin-only UI (like Mail Server Settings) from regular users. It's
    // a plain String here (not the backend's Role enum), which Jackson
    // deserializes from the JSON string with no extra config needed.
    private String role;
}
