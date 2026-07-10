package com.roze.thundercall.api.dto;

import com.roze.thundercall.api.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private Long id;
    private String token;
    private String refreshToken;
    private String username;
    private String email;
    private Role role;
}
