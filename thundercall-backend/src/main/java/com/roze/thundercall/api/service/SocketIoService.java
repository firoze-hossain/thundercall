package com.roze.thundercall.api.service;

import com.roze.thundercall.api.dto.SocketIoConnectRequest;
import com.roze.thundercall.api.dto.SocketIoEmitRequest;
import com.roze.thundercall.api.dto.SocketIoStatusResponse;
import com.roze.thundercall.api.entity.User;

public interface SocketIoService {
    SocketIoStatusResponse connect(SocketIoConnectRequest request, User owner);

    /** Returns the session's current status plus every event logged
     * since the last poll (sinceIndex is the count the caller already
     * has — simpler and more robust across restarts than timestamps). */
    SocketIoStatusResponse poll(String sessionId, int sinceIndex, User owner);

    void emit(String sessionId, SocketIoEmitRequest request, User owner);

    void disconnect(String sessionId, User owner);
}