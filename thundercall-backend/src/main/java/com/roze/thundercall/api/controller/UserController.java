package com.roze.thundercall.api.controller;

import com.roze.thundercall.api.dto.UpdateProfileRequest;
import com.roze.thundercall.api.dto.UserProfileResponse;
import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.service.AuthService;
import com.roze.thundercall.api.service.UserProfileService;
import com.roze.thundercall.api.utils.BaseController;
import com.roze.thundercall.api.utils.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController extends BaseController {
    private final UserProfileService userProfileService;
    private final AuthService authService;

    @GetMapping("/me")
    public ResponseEntity<BaseResponse<UserProfileResponse>> getMyProfile(Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return ok(userProfileService.getProfile(user));
    }

    @PutMapping("/me")
    public ResponseEntity<BaseResponse<UserProfileResponse>> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request, Authentication authentication) {
        User user = authService.getUserFromAuthentication(authentication);
        return ok(userProfileService.updateProfile(user, request), "Profile updated");
    }
}
