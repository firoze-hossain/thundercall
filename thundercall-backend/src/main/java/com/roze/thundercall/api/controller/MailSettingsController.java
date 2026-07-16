package com.roze.thundercall.api.controller;

import com.roze.thundercall.api.dto.MailSettingsRequest;
import com.roze.thundercall.api.dto.MailSettingsResponse;
import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.service.AuthService;
import com.roze.thundercall.api.service.MailSettingsService;
import com.roze.thundercall.api.utils.BaseController;
import com.roze.thundercall.api.utils.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mail-settings")
public class MailSettingsController extends BaseController {
    private final MailSettingsService mailSettingsService;
    private final AuthService authService;

    @GetMapping("")
    public ResponseEntity<BaseResponse<MailSettingsResponse>> getSettings(Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return ok(mailSettingsService.getSettings(user));
    }

    @PutMapping("")
    public ResponseEntity<BaseResponse<MailSettingsResponse>> updateSettings(
            @Valid @RequestBody MailSettingsRequest request, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return ok(mailSettingsService.updateSettings(request, user), "Mail settings saved");
    }

    @PostMapping("/test")
    public ResponseEntity<BaseResponse<Void>> sendTestEmail(
            @RequestParam String toAddress, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        mailSettingsService.sendTestEmail(toAddress, user);
        return noContent("Test email sent to " + toAddress);
    }
}
