package com.roze.thundercall.service.impl;

import com.roze.thundercall.dto.AuthResponse;
import com.roze.thundercall.dto.LoginRequest;
import com.roze.thundercall.dto.RegisterRequest;
import com.roze.thundercall.entity.RefreshToken;
import com.roze.thundercall.entity.User;
import com.roze.thundercall.exception.AuthException;
import com.roze.thundercall.exception.ResourceExistException;
import com.roze.thundercall.exception.ResourceNotFoundException;
import com.roze.thundercall.mapper.UserMapper;
import com.roze.thundercall.repository.UserRepository;
import com.roze.thundercall.security.JwtTokenProvider;
import com.roze.thundercall.security.RefreshTokenService;
import com.roze.thundercall.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new ResourceExistException("Username already exist");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ResourceExistException("Email already exist");
        }
        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(user);
        String token = jwtTokenProvider.generateToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user.getId()).getToken();
        return userMapper.toResponse(user, token, refreshToken);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameOrEmail(request.usernameOrEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username or email: " + request.usernameOrEmail()));
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
        String username = ((org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal()).getUsername();
        return userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
