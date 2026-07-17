package com.roze.thundercall.api.enums;

public enum HttpMethod {
    GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS,
    // Not a real HTTP verb — lets a WebSocket request be saved/loaded
    // through the same collection/request storage as everything else.
    // executeRequest() rejects it before it ever reaches an actual HTTP
    // call, since the client connects directly for WS (see
    // MainController's WS handling) rather than proxying through here.
    WS
}