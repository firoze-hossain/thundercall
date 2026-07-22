package com.roze.thundercall.ui.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfile {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String avatarBase64;
    private String role;

    /** What the UI should call this person: full name if set, else username. */
    public String displayName() {
        if (fullName != null && !fullName.isBlank()) {
            return fullName;
        }
        return username;
    }
}
