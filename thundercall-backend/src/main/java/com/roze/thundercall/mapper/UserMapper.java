package com.roze.thundercall.mapper;

import com.roze.thundercall.dto.AuthResponse;
import com.roze.thundercall.dto.RegisterRequest;
import com.roze.thundercall.entity.User;
import com.roze.thundercall.enums.Role;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public User toEntity(RegisterRequest request) {
        User user = new User();
        user.setEmail(request.email());
        user.setUsername(request.username());
        user.setPassword(request.password());
        user.setEnabled(true);
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
