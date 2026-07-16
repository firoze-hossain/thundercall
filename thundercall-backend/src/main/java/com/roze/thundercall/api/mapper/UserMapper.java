package com.roze.thundercall.api.mapper;

import com.roze.thundercall.api.dto.AuthResponse;
import com.roze.thundercall.api.dto.RegisterRequest;
import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.enums.Role;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public User toEntity(RegisterRequest request) {
        User user = new User();
        user.setEmail(request.email());
        user.setUsername(request.username());
        user.setPassword(request.password());
        user.setEnabled(true);
        user.setEmailVerified(false);
        user.setRole(Role.USER);
        return user;
    }

    public AuthResponse toResponse(User user, String token, String refreshToken) {
        AuthResponse response = new AuthResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());
        response.setToken(token);
        response.setRefreshToken(refreshToken);
        return response;

    }
}
