package com.roze.thundercall.api.controller;

import com.roze.thundercall.api.dto.OnlineUserResponse;
import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.service.AuthService;
import com.roze.thundercall.api.service.PresenceService;
import com.roze.thundercall.api.utils.BaseController;
import com.roze.thundercall.api.utils.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/presence")
public class PresenceController extends BaseController {
    private final PresenceService presenceService;
    private final AuthService authService;

    /** Marks the calling user as online. The desktop client sends this
     * every ~25s while running; missing two heartbeats = shown offline. */
    @PostMapping("/heartbeat")
    public ResponseEntity<BaseResponse<Void>> heartbeat(Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        presenceService.heartbeat(user);
        return noContent("Online");
    }

    /** Online users related to the caller (teammates + workspace-sharing
     * connections). Powers the green presence bubbles in the client. */
    @GetMapping("/online")
    public ResponseEntity<BaseResponse<List<OnlineUserResponse>>> getOnlineUsers(Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return ok(presenceService.getOnlineRelatedUsers(user));
    }
}
