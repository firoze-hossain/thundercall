package com.roze.thundercall.api.service;

import com.roze.thundercall.api.dto.UpdateProfileRequest;
import com.roze.thundercall.api.dto.UserProfileResponse;
import com.roze.thundercall.api.entity.User;

public interface UserProfileService {
    UserProfileResponse getProfile(User user);

    UserProfileResponse updateProfile(User user, UpdateProfileRequest request);
}
