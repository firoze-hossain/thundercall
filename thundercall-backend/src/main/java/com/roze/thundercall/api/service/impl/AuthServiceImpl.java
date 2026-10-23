package com.roze.thundercall.api.service.impl;

import com.roze.thundercall.api.dto.AuthResponse;
import com.roze.thundercall.api.dto.LoginRequest;
import com.roze.thundercall.api.dto.RegisterRequest;
import com.roze.thundercall.api.dto.RegisterResponse;
import com.roze.thundercall.api.entity.RefreshToken;
import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.exception.AuthException;
import com.roze.thundercall.api.exception.EmailNotVerifiedException;
import com.roze.thundercall.api.exception.ResourceExistException;
import com.roze.thundercall.api.exception.ResourceNotFoundException;
import com.roze.thundercall.api.mapper.UserMapper;
import com.roze.thundercall.api.repository.UserRepository;
import com.roze.thundercall.api.security.JwtTokenProvider;
import com.roze.thundercall.api.security.RefreshTokenService;
import com.roze.thundercall.api.service.AuthService;
import com.roze.thundercall.api.service.EmailService;
import com.roze.thundercall.api.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * FIX: register() now creates the user's default workspace (with the
 * "Getting started" sample collection) in the same transaction. New users
 * can create collections, environments and send requests immediately after
 * login — no setup dialog, no "No workspace found" errors.
 *
 * UPDATE: registration no longer auto-logs-in. A verified email is now
 * mandatory — register() creates the account disabled-for-login
 * (emailVerified=false), emails a 6-digit code, and only verifyEmail()
 * issues a token. This matters for team invitations too: an invited
 * address needs to actually belong to whoever is joining the team.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private static final int VERIFICATION_CODE_MINUTES_VALID = 15;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final WorkspaceService workspaceService;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new ResourceExistException("Username already exist");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ResourceExistException("Email already exist");
        }
        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        String code = generateVerificationCode();
        user.setVerificationCode(code);
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(VERIFICATION_CODE_MINUTES_VALID));
        user = userRepository.save(user);

        // Every new account gets a ready-to-use workspace with sample requests,
        // set up right away so it's waiting the moment they verify and log in.
        workspaceService.getOrCreateDefaultWorkspace(user);

        sendVerificationEmail(user.getEmail(), user.getUsername(), code);

        return new RegisterResponse(user.getUsername(), user.getEmail(), true,
                "We've sent a 6-digit code to " + user.getEmail() + " — enter it to finish creating your account.");
    }

    @Override
    @Transactional
    public AuthResponse verifyEmail(String email, String code) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found for that email"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            // Already verified (e.g. they double-submitted) — just log them in
            // rather than treating this as an error.
            return issueTokens(user);
        }
        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(code)) {
            throw new AuthException("That code doesn't match — check the email and try again.");
        }
        if (user.getVerificationCodeExpiresAt() == null
                || user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AuthException("That code has expired — request a new one.");
        }

        user.setEmailVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);

        return issueTokens(user);
    }

    @Override
    @Transactional
    public void resendVerificationCode(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found for that email"));
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new AuthException("This email is already verified — you can just log in.");
        }
        String code = generateVerificationCode();
        user.setVerificationCode(code);
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(VERIFICATION_CODE_MINUTES_VALID));
        userRepository.save(user);
        sendVerificationEmail(user.getEmail(), user.getUsername(), code);
    }

    private record RozeHubUser(Long id, String name, String email, String accountType) {}

    private RozeHubUser verifyWithRozeHub(String login, String password) {
        try {
            java.net.URL url = new java.net.URL("http://localhost:8000/api/v1/auth/verify");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            conn.setDoOutput(true);

            String body = String.format("{\"login\":\"%s\",\"password\":\"%s\"}",
                    login.replace("\\", "\\\\").replace("\"", "\\\""),
                    password.replace("\\", "\\\\").replace("\"", "\\\""));

            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                try (java.io.InputStream is = conn.getInputStream()) {
                    String json = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
                    if (root.path("success").asBoolean()) {
                        com.fasterxml.jackson.databind.JsonNode userNode = root.path("user");
                        return new RozeHubUser(
                                userNode.path("id").asLong(),
                                userNode.path("name").asText(),
                                userNode.path("email").asText(),
                                userNode.path("account_type").asText("user")
                        );
                    }
                }
            }
        } catch (Exception e) {
            log.warn("RozeHub auth verification failed or unreachable: {}", e.getMessage());
        }
        return null;
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.usernameOrEmail().trim();
        User user = userRepository.findByUsernameOrEmail(identifier).orElse(null);

        boolean passwordMatches = (user != null && passwordEncoder.matches(request.password(), user.getPassword()));

        if (!passwordMatches) {
            // Check RozeHub central user account
            RozeHubUser rozeUser = verifyWithRozeHub(identifier, request.password());
            if (rozeUser != null) {
                if (user == null) {
                    user = userRepository.findByEmailIgnoreCase(rozeUser.email()).orElse(null);
                }
                if (user == null) {
                    // Provision new user from RozeHub
                    String baseUsername = (rozeUser.name() != null && !rozeUser.name().isBlank())
                            ? rozeUser.name().trim()
                            : rozeUser.email().split("@")[0];
                    String username = baseUsername;
                    int counter = 1;
                    while (userRepository.existsByUsernameIgnoreCase(username)) {
                        username = baseUsername + counter++;
                    }

                    user = User.builder()
                            .username(username)
                            .email(rozeUser.email().trim().toLowerCase())
                            .password(passwordEncoder.encode(request.password()))
                            .enabled(true)
                            .emailVerified(true)
                            .role(com.roze.thundercall.api.enums.Role.USER)
                            .build();
                    user = userRepository.save(user);
                    workspaceService.getOrCreateDefaultWorkspace(user);
                } else {
                    // Sync password and verification from RozeHub
                    user.setPassword(passwordEncoder.encode(request.password()));
                    user.setEmailVerified(true);
                    user = userRepository.save(user);
                }
            } else {
                if (user == null) {
                    throw new UsernameNotFoundException("User not found with username or email: " + request.usernameOrEmail());
                } else {
                    throw new AuthException("Invalid credentials");
                }
            }
        }

        if (!user.getEnabled()) {
            throw new AuthException("User account is disabled");
        }
        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            throw new EmailNotVerifiedException("Please verify your email before logging in — check your inbox "
                    + "for the code, or request a new one.", user.getEmail());
        }
        return issueTokens(user);
    }

    @Override
    @Transactional
    public AuthResponse ssoLogin(com.roze.thundercall.api.dto.SsoLoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            String baseUsername = (request.username() != null && !request.username().isBlank())
                    ? request.username().trim()
                    : email.split("@")[0];
            String username = baseUsername;
            int counter = 1;
            while (userRepository.existsByUsernameIgnoreCase(username)) {
                username = baseUsername + counter++;
            }

            user = User.builder()
                    .username(username)
                    .email(email)
                    .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                    .enabled(true)
                    .emailVerified(true)
                    .role(com.roze.thundercall.api.enums.Role.USER)
                    .build();
            user = userRepository.save(user);
            workspaceService.getOrCreateDefaultWorkspace(user);
        } else {
            if (!Boolean.TRUE.equals(user.getEmailVerified())) {
                user.setEmailVerified(true);
                user = userRepository.save(user);
            }
        }
        return issueTokens(user);
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
    public void logout(String refreshToken) {
        refreshTokenService.deleteByToken(refreshToken);
    }

    @Override
    public User getUserFromAuthentication(Authentication authentication) {
        String username = ((org.springframework.security.core.userdetails.UserDetails)
                authentication.getPrincipal()).getUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private AuthResponse issueTokens(User user) {
        String token = jwtTokenProvider.generateToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user.getId()).getToken();
        return userMapper.toResponse(user, token, refreshToken);
    }

    private String generateVerificationCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private void sendVerificationEmail(String email, String username, String code) {
        try {
            emailService.send(email, "Verify your Thundercall account",
                    "Hi " + username + ",\n\n"
                            + "Your verification code is: " + code + "\n\n"
                            + "This code expires in " + VERIFICATION_CODE_MINUTES_VALID + " minutes. "
                            + "Enter it in Thundercall to finish setting up your account.\n\n"
                            + "If you didn't request this, you can ignore this email.");
        } catch (Exception e) {
            // Don't leave an unverifiable account behind if mail genuinely
            // isn't configured — surface it clearly instead of a silent
            // account nobody can ever activate.
            log.error("Could not send verification email to {}: {}", email, e.getMessage());
            throw new AuthException("Your account was created, but the verification email couldn't be sent "
                    + "(" + e.getMessage() + "). Contact the app administrator to check the mail settings, "
                    + "then use \"Resend code\" once that's fixed.");
        }
    }
}