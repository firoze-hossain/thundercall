package com.roze.thundercall.api.service.impl;

import com.roze.thundercall.api.dto.UpdateProfileRequest;
import com.roze.thundercall.api.dto.UserProfileResponse;
import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.repository.UserRepository;
import com.roze.thundercall.api.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(User user) {
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(User user, UpdateProfileRequest request) {
        if (request.fullName() != null) {
            String trimmed = request.fullName().trim();
            user.setFullName(trimmed.isEmpty() ? null : trimmed);
        }
        if (request.removeAvatar()) {
            user.setAvatar(null);
        } else if (request.avatarBase64() != null && !request.avatarBase64().isBlank()) {
            // Reject anything that isn't real base64 up front — a corrupt
            // value here would otherwise break every client that tries to
            // render this user's avatar later.
            try {
                Base64.getDecoder().decode(request.avatarBase64());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Avatar image data is not valid");
            }
            user.setAvatar(request.avatarBase64());
        }
        return toResponse(userRepository.save(user));
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatar(),
                user.getRole());
    }
}
