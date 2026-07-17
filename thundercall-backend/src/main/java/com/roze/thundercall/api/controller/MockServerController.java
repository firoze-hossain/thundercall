package com.roze.thundercall.api.controller;

import com.roze.thundercall.api.dto.*;
import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.service.AuthService;
import com.roze.thundercall.api.service.MockServerService;
import com.roze.thundercall.api.utils.BaseController;
import com.roze.thundercall.api.utils.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mock-servers")
public class MockServerController extends BaseController {
    private final MockServerService mockServerService;
    private final AuthService authService;

    @PostMapping("")
    public ResponseEntity<BaseResponse<MockServerResponse>> createMockServer(
            @Valid @RequestBody MockServerRequest request, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return created(mockServerService.createMockServer(request, user), "Mock server created");
    }

    @GetMapping("")
    public ResponseEntity<BaseResponse<List<MockServerResponse>>> getMyMockServers(Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return ok(mockServerService.getMyMockServers(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<MockServerResponse>> getMockServer(
            @PathVariable Long id, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return ok(mockServerService.getMockServer(id, user));
    }

    @PutMapping("/{id}/enabled")
    public ResponseEntity<BaseResponse<Void>> setEnabled(
            @PathVariable Long id, @RequestParam boolean enabled, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        mockServerService.setEnabled(id, enabled, user);
        return noContent(enabled ? "Mock server enabled" : "Mock server disabled");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> deleteMockServer(
            @PathVariable Long id, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        mockServerService.deleteMockServer(id, user);
        return noContent("Mock server deleted");
    }

    @PostMapping("/{id}/routes")
    public ResponseEntity<BaseResponse<MockRouteResponse>> addRoute(
            @PathVariable Long id, @Valid @RequestBody MockRouteRequest request, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return created(mockServerService.addRoute(id, request, user), "Route added");
    }

    @GetMapping("/{id}/routes")
    public ResponseEntity<BaseResponse<List<MockRouteResponse>>> getRoutes(
            @PathVariable Long id, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return ok(mockServerService.getRoutes(id, user));
    }

    @PutMapping("/{id}/routes/{routeId}")
    public ResponseEntity<BaseResponse<MockRouteResponse>> updateRoute(
            @PathVariable Long id, @PathVariable Long routeId,
            @Valid @RequestBody MockRouteRequest request, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return ok(mockServerService.updateRoute(id, routeId, request, user), "Route updated");
    }

    @DeleteMapping("/{id}/routes/{routeId}")
    public ResponseEntity<BaseResponse<Void>> deleteRoute(
            @PathVariable Long id, @PathVariable Long routeId, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        mockServerService.deleteRoute(id, routeId, user);
        return noContent("Route deleted");
    }
}