package com.roze.thundercall.api.service.impl;

import com.roze.thundercall.api.dto.AuthResponse;
import com.roze.thundercall.api.dto.LoginRequest;
import com.roze.thundercall.api.dto.RegisterRequest;
import com.roze.thundercall.api.entity.RefreshToken;
import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.exception.AuthException;
import com.roze.thundercall.api.exception.ResourceExistException;
import com.roze.thundercall.api.exception.ResourceNotFoundException;
import com.roze.thundercall.api.mapper.UserMapper;
import com.roze.thundercall.api.repository.UserRepository;
import com.roze.thundercall.api.security.JwtTokenProvider;
import com.roze.thundercall.api.security.RefreshTokenService;
import com.roze.thundercall.api.service.AuthService;
import com.roze.thundercall.api.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FIX: register() now creates the user's default workspace (with the
 * "Getting started" sample collection) in the same transaction. New users
 * can create collections, environments and send requests immediately after
 * login — no setup dialog, no "No workspace found" errors.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final WorkspaceService workspaceService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new ResourceExistException("Username already exist");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ResourceExistException("Email already exist");
        }
        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        user = userRepository.save(user);

        // Every new account gets a ready-to-use workspace with sample requests
        workspaceService.getOrCreateDefaultWorkspace(user);

        String token = jwtTokenProvider.generateToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user.getId()).getToken();
        return userMapper.toResponse(user, token, refreshToken);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameOrEmail(request.usernameOrEmail())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username or email: " + request.usernameOrEmail()));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new AuthException("Invalid credentials");
        }
        if (!user.getEnabled()) {
            throw new AuthException("User account is disabled");
        }
        String token = jwtTokenProvider.generateToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user.getId()).getToken();
        return userMapper.toResponse(user, token, refreshToken);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        return refreshTokenService.findByToken(refreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String token = jwtTokenProvider.generateToken(user);
                    return userMapper.toResponse(user, token, refreshToken);
                }).orElseThrow(() -> new AuthException("Invalid Refresh Token"));
    }

    @Override
    public void logout(String token) {
        refreshTokenService.deleteByToken(token);
    }

    @Override
    public User getUserFromAuthentication(Authentication authentication) {
        String username = ((org.springframework.security.core.userdetails.UserDetails)
                authentication.getPrincipal()).getUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}