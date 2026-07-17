package com.roze.thundercall.api.dto;

import java.util.List;

public record SocketIoStatusResponse(
        String sessionId,
        String status,
        List<SocketIoEventResponse> events
) {
}