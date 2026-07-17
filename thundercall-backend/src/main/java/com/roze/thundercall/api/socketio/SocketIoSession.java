package com.roze.thundercall.api.socketio;

import io.socket.client.Socket;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public class SocketIoSession {
    private final String sessionId;
    private final Long ownerId;
    private final List<SocketIoEvent> events = new CopyOnWriteArrayList<>();
    @Setter
    private volatile Socket socket;
    @Setter
    private volatile String status = "connecting"; // connecting, connected, disconnected, error

    public SocketIoSession(String sessionId, Long ownerId) {
        this.sessionId = sessionId;
        this.ownerId = ownerId;
    }

    /** Callbacks from the socket.io-client library fire on its own
     * internal threads, never the request thread — events list is a
     * CopyOnWriteArrayList specifically so this is safe to call from
     * anywhere without extra synchronization. */
    public void addEvent(String direction, String eventName, String data) {
        events.add(new SocketIoEvent(direction, eventName, data, System.currentTimeMillis()));
    }
}