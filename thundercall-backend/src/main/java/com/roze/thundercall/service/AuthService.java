package com.roze.thundercall.service;

import com.roze.thundercall.dto.AuthResponse;
import com.roze.thundercall.dto.LoginRequest;
import com.roze.thundercall.dto.RegisterRequest;
import com.roze.thundercall.entity.User;
import org.springframework.security.core.Authentication;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(String refreshToken);

    void logout(String token);

    User getUserFromAuthentication(Authentication authentication);
}
