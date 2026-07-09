package com.roze.thundercall.controller;

import com.roze.thundercall.dto.AuthResponse;
import com.roze.thundercall.dto.LoginRequest;
import com.roze.thundercall.dto.RegisterRequest;
import com.roze.thundercall.service.AuthService;
import com.roze.thundercall.utils.BaseResponse;
import com.roze.thundercall.utils.BaseController;
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
    public ResponseEntity<BaseResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return created(response);
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
    public ResponseEntity<BaseResponse<Void>> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token.substring(7));
        return noContent("Logout successfully");
    }
}
