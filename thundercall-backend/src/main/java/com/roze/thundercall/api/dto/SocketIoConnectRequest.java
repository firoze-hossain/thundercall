package com.roze.thundercall.api.dto;

import jakarta.validation.constraints.NotBlank;

public record SocketIoConnectRequest(
        @NotBlank String url,
        // Optional — defaults to "/" (Socket.IO's default namespace) if blank.
        String namespace
) {
}