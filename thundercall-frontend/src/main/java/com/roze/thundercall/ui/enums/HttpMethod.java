package com.roze.thundercall.ui.enums;

public enum HttpMethod {
    GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS,
    // Not a real HTTP verb — used only to mark a request as a
    // WebSocket connection so it can be saved/loaded like any other
    // request. Never sent through the normal HTTP execute path.
    WS
}