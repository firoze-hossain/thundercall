package com.roze.thundercall.api.service;

import com.roze.thundercall.api.dto.AuthResponse;
import com.roze.thundercall.api.dto.LoginRequest;
import com.roze.thundercall.api.dto.RegisterRequest;
import com.roze.thundercall.api.entity.User;
import org.springframework.security.core.Authentication;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(String refreshToken);

    void logout(String refreshToken);

    User getUserFromAuthentication(Authentication authentication);
}
