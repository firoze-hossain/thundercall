package com.roze.thundercall.ui.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.roze.thundercall.ui.models.BaseResponse;
import com.roze.thundercall.ui.models.UpdateProfileRequest;
import com.roze.thundercall.ui.models.UserProfile;

import java.io.IOException;
import java.util.Optional;

/** The logged-in user's own profile: full name and avatar. */
public class UserService {
    private static final String BASE_URL = "/users";

    public static Optional<UserProfile> getMe() {
        try {
            BaseResponse<UserProfile> response = ApiClient.get(BASE_URL + "/me",
                    new TypeReference<BaseResponse<UserProfile>>() {
                    });
            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }
        } catch (IOException e) {
            System.err.println("Failed to load profile: " + e.getMessage());
        }
        return Optional.empty();
    }

    public static Optional<UserProfile> updateMe(UpdateProfileRequest request) throws IOException {
        BaseResponse<UserProfile> response = ApiClient.put(BASE_URL + "/me", request,
                new TypeReference<BaseResponse<UserProfile>>() {
                });
        if (response != null && response.isSuccess()) {
            return Optional.ofNullable(response.getData());
        }
        throw new IOException(response != null && response.getMessage() != null
                ? response.getMessage() : "Couldn't update your profile");
    }
}
