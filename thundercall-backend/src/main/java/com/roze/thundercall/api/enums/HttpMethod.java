package com.roze.thundercall.api.enums;

public enum HttpMethod {
    GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS,
    // Not a real HTTP verb — lets a WebSocket request be saved/loaded
    // through the same collection/request storage as everything else.
    // executeRequest() rejects it before it ever reaches an actual HTTP
    // call, since the client connects directly for WS (see
    // MainController's WS handling) rather than proxying through here.
    WS,
    // Same idea as WS — marks a request as a Socket.IO connection.
    // Unlike WS, Socket.IO connections DO go through the backend (see
    // SocketIoController) since they use the real socket.io-client
    // library, which isn't something the JavaFX client can depend on
    // directly without JPMS module complications.
    SOCKETIO
}