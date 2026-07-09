package com.roze.thundercall.controller;

import com.roze.thundercall.dto.EnvironmentRequest;
import com.roze.thundercall.dto.EnvironmentResponse;
import com.roze.thundercall.entity.User;
import com.roze.thundercall.service.AuthService;
import com.roze.thundercall.service.EnvironmentService;
import com.roze.thundercall.utils.BaseController;
import com.roze.thundercall.utils.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/environments")
@RequiredArgsConstructor
public class EnvironmentController extends BaseController {
    private final EnvironmentService environmentService;
    private final AuthService authService;

    @PostMapping("")
    public ResponseEntity<BaseResponse<EnvironmentResponse>> createEnvironment(
            @Valid @RequestBody EnvironmentRequest request, 
            Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        EnvironmentResponse response = environmentService.createEnvironment(request, user);
        return created(response, "Environment created successfully");
    }

    @GetMapping("")
    public ResponseEntity<BaseResponse<List<EnvironmentResponse>>> getUserEnvironments(Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        List<EnvironmentResponse> responses = environmentService.getUserEnvironments(user);
        return ok(responses);
    }

    @GetMapping("/active")
    public ResponseEntity<BaseResponse<List<EnvironmentResponse>>> getActiveEnvironments(Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        List<EnvironmentResponse> responses = environmentService.getActiveEnvironments(user);
        return ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<EnvironmentResponse>> getEnvironment(
            @PathVariable Long id, 
            Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        EnvironmentResponse response = environmentService.getEnvironmentById(id, user);
        return ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<EnvironmentResponse>> updateEnvironment(
            @PathVariable Long id, 
            @Valid @RequestBody EnvironmentRequest request, 
            Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        EnvironmentResponse response = environmentService.updateEnvironment(id, request, user);
        return ok(response, "Environment updated successfully");
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<BaseResponse<EnvironmentResponse>> toggleEnvironmentStatus(
            @PathVariable Long id, 
            @RequestParam Boolean active, 
            Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        EnvironmentResponse response = environmentService.toggleEnvironmentStatus(id, user, active);
        return ok(response, "Environment status updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> deleteEnvironment(
            @PathVariable Long id, 
            Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        environmentService.deleteEnvironment(id, user);
        return noContent("Environment deleted successfully");
    }
}