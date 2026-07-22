package com.roze.thundercall.ui.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OnlineUser {
    private Long userId;
    private String username;
    private String fullName;

    public String displayName() {
        if (fullName != null && !fullName.isBlank()) {
            return fullName;
        }
        return username;
    }
}
