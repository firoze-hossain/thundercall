package com.roze.thundercall.api.dto;

public record SocketIoEventResponse(
        String direction,
        String eventName,
        String data,
        long timestamp
) {
}