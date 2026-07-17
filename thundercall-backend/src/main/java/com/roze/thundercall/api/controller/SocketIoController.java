package com.roze.thundercall.api.controller;

import com.roze.thundercall.api.dto.SocketIoConnectRequest;
import com.roze.thundercall.api.dto.SocketIoEmitRequest;
import com.roze.thundercall.api.dto.SocketIoStatusResponse;
import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.service.AuthService;
import com.roze.thundercall.api.service.SocketIoService;
import com.roze.thundercall.api.utils.BaseController;
import com.roze.thundercall.api.utils.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** The desktop client's own account authenticates these — unlike a mock
 * server, a Socket.IO connection is something YOU initiate against
 * someone else's server, not something external tools connect to, so
 * there's no public/unauthenticated counterpart here. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/socketio")
public class SocketIoController extends BaseController {
    private final SocketIoService socketIoService;
    private final AuthService authService;

    @PostMapping("/connect")
    public ResponseEntity<BaseResponse<SocketIoStatusResponse>> connect(
            @Valid @RequestBody SocketIoConnectRequest request, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return created(socketIoService.connect(request, user), "Connecting...");
    }

    @GetMapping("/{sessionId}/poll")
    public ResponseEntity<BaseResponse<SocketIoStatusResponse>> poll(
            @PathVariable String sessionId, @RequestParam(defaultValue = "0") int since,
            Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return ok(socketIoService.poll(sessionId, since, user));
    }

    @PostMapping("/{sessionId}/emit")
    public ResponseEntity<BaseResponse<Void>> emit(
            @PathVariable String sessionId, @Valid @RequestBody SocketIoEmitRequest request,
            Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        socketIoService.emit(sessionId, request, user);
        return noContent("Sent");
    }

    @PostMapping("/{sessionId}/disconnect")
    public ResponseEntity<BaseResponse<Void>> disconnect(
            @PathVariable String sessionId, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        socketIoService.disconnect(sessionId, user);
        return noContent("Disconnected");
    }
}