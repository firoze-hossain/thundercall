package com.roze.thundercall.api.dto;

public record OnlineUserResponse(
        Long userId,
        String username,
        String fullName
) {
}
