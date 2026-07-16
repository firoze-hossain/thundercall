package com.roze.thundercall.api.service;

import com.roze.thundercall.api.dto.AuthResponse;
import com.roze.thundercall.api.dto.LoginRequest;
import com.roze.thundercall.api.dto.RegisterRequest;
import com.roze.thundercall.api.dto.RegisterResponse;
import com.roze.thundercall.api.entity.User;
import org.springframework.security.core.Authentication;

public interface AuthService {
    RegisterResponse register(RegisterRequest request);

    AuthResponse verifyEmail(String email, String code);

    void resendVerificationCode(String email);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(String refreshToken);

    void logout(String refreshToken);

    User getUserFromAuthentication(Authentication authentication);
}
