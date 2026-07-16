package com.roze.thundercall.api.controller;

import com.roze.thundercall.api.dto.AuthResponse;
import com.roze.thundercall.api.dto.LoginRequest;
import com.roze.thundercall.api.dto.RegisterRequest;
import com.roze.thundercall.api.dto.RegisterResponse;
import com.roze.thundercall.api.dto.ResendVerificationRequest;
import com.roze.thundercall.api.dto.VerifyEmailRequest;
import com.roze.thundercall.api.service.AuthService;
import com.roze.thundercall.api.utils.BaseResponse;
import com.roze.thundercall.api.utils.BaseController;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController extends BaseController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<BaseResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return created(response, response.message());
    }

    @PostMapping("/verify-email")
    public ResponseEntity<BaseResponse<AuthResponse>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        AuthResponse response = authService.verifyEmail(request.email(), request.code());
        return ok(response, "Email verified — welcome to Thundercall!");
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<BaseResponse<Void>> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerificationCode(request.email());
        return noContent("A new code has been sent to " + request.email());
    }

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<BaseResponse<AuthResponse>> refreshToken(@RequestParam String refreshToken) {
        return ok(authService.refreshToken(refreshToken), "Token Refreshed Successfully");
    }

    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<Void>> logout(@RequestParam String refreshToken) {
        authService.logout(refreshToken);
        return noContent("Logout successfully");
    }
}
