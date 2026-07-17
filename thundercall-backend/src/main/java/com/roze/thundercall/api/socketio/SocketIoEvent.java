package com.roze.thundercall.api.socketio;

import lombok.AllArgsConstructor;
import lombok.Data;

/** One entry in a session's event log — a received event, a sent
 * (emitted) event, or a connection-lifecycle marker (connect/
 * disconnect/error). Kept in memory only; these are live, transient
 * sessions, not something that needs to survive an app restart. */
@Data
@AllArgsConstructor
public class SocketIoEvent {
    private String direction; // "in", "out", or "system"
    private String eventName;
    private String data;
    private long timestamp;
}