package com.roze.thundercall.api.service.impl;

import com.roze.thundercall.api.dto.SocketIoConnectRequest;
import com.roze.thundercall.api.dto.SocketIoEmitRequest;
import com.roze.thundercall.api.dto.SocketIoEventResponse;
import com.roze.thundercall.api.dto.SocketIoStatusResponse;
import com.roze.thundercall.api.entity.User;
import com.roze.thundercall.api.exception.AuthException;
import com.roze.thundercall.api.exception.ResourceNotFoundException;
import com.roze.thundercall.api.service.SocketIoService;
import com.roze.thundercall.api.socketio.SocketIoSession;
import io.socket.client.IO;
import io.socket.client.Socket;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Real Socket.IO connections (via the official io.socket:socket.io-client
 * library — see the pom.xml comment for why hand-rolling the protocol
 * wasn't the right call), held in memory for the app's lifetime. The
 * desktop client polls poll() periodically rather than holding its own
 * live connection to this service — a deliberately simple, robust relay
 * instead of a second streaming layer on top of the actual Socket.IO
 * connection this service already maintains. */
@Service
@Slf4j
public class SocketIoServiceImpl implements SocketIoService {
    private final Map<String, SocketIoSession> sessions = new ConcurrentHashMap<>();

    @Override
    public SocketIoStatusResponse connect(SocketIoConnectRequest request, User owner) {
        String sessionId = UUID.randomUUID().toString();
        SocketIoSession session = new SocketIoSession(sessionId, owner.getId());
        sessions.put(sessionId, session);

        String fullUrl = buildUrl(request.url(), request.namespace());
        try {
            Socket socket = IO.socket(fullUrl);
            session.setSocket(socket);

            socket.on(Socket.EVENT_CONNECT, args -> {
                session.setStatus("connected");
                session.addEvent("system", "connect", "{}");
            });
            socket.on(Socket.EVENT_DISCONNECT, args -> {
                session.setStatus("disconnected");
                session.addEvent("system", "disconnect", args.length > 0 ? String.valueOf(args[0]) : "");
            });
            socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
                session.setStatus("error");
                session.addEvent("system", "connect_error", args.length > 0 ? String.valueOf(args[0]) : "");
            });
            // Catch-all — logs every named event the server sends, without
            // requiring the caller to know event names in advance. The
            // first arg is the event name; the rest is its payload.
            socket.onAnyIncoming(args -> {
                if (args.length == 0) {
                    return;
                }
                String eventName = String.valueOf(args[0]);
                Object[] payload = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new Object[0];
                session.addEvent("in", eventName, describePayload(payload));
            });

            socket.connect();
        } catch (URISyntaxException e) {
            session.setStatus("error");
            session.addEvent("system", "connect_error", "Invalid URL: " + e.getMessage());
        } catch (Exception e) {
            log.warn("Socket.IO connect failed for session {}: {}", sessionId, e.getMessage());
            session.setStatus("error");
            session.addEvent("system", "connect_error", e.getMessage());
        }

        return toResponse(session, 0);
    }

    @Override
    public SocketIoStatusResponse poll(String sessionId, int sinceIndex, User owner) {
        SocketIoSession session = findOwned(sessionId, owner);
        return toResponse(session, sinceIndex);
    }

    @Override
    public void emit(String sessionId, SocketIoEmitRequest request, User owner) {
        SocketIoSession session = findOwned(sessionId, owner);
        Socket socket = session.getSocket();
        if (socket == null || !"connected".equals(session.getStatus())) {
            throw new IllegalStateException("Not connected");
        }
        Object payload = parsePayload(request.data());
        if (payload == null) {
            socket.emit(request.event());
        } else {
            socket.emit(request.event(), payload);
        }
        session.addEvent("out", request.event(), request.data() == null ? "" : request.data());
    }

    @Override
    public void disconnect(String sessionId, User owner) {
        SocketIoSession session = findOwned(sessionId, owner);
        if (session.getSocket() != null) {
            session.getSocket().disconnect();
        }
        session.setStatus("disconnected");
        sessions.remove(sessionId);
    }

    private SocketIoSession findOwned(String sessionId, User owner) {
        SocketIoSession session = sessions.get(sessionId);
        if (session == null) {
            throw new ResourceNotFoundException("Socket.IO session not found (it may have been disconnected)");
        }
        if (!session.getOwnerId().equals(owner.getId())) {
            throw new AuthException("This session doesn't belong to you");
        }
        return session;
    }

    private String buildUrl(String baseUrl, String namespace) {
        if (namespace == null || namespace.isBlank() || namespace.equals("/")) {
            return baseUrl;
        }
        String ns = namespace.startsWith("/") ? namespace : "/" + namespace;
        String trimmedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return trimmedBase + ns;
    }

    /** Sent data may be a JSON object/array, or just a plain string —
     * both are valid emit payloads. */
    private Object parsePayload(String data) {
        if (data == null || data.isBlank()) {
            return null;
        }
        String trimmed = data.trim();
        try {
            if (trimmed.startsWith("{")) {
                return new JSONObject(trimmed);
            }
            if (trimmed.startsWith("[")) {
                return new JSONArray(trimmed);
            }
        } catch (Exception ignored) {
            // not valid JSON — fall through and send as plain text
        }
        return trimmed;
    }

    private String describePayload(Object[] payload) {
        if (payload.length == 0) {
            return "";
        }
        if (payload.length == 1) {
            return String.valueOf(payload[0]);
        }
        return Arrays.toString(payload);
    }

    private SocketIoStatusResponse toResponse(SocketIoSession session, int sinceIndex) {
        List<SocketIoEventResponse> events = session.getEvents().stream()
                .skip(Math.max(sinceIndex, 0))
                .map(e -> new SocketIoEventResponse(e.getDirection(), e.getEventName(), e.getData(), e.getTimestamp()))
                .toList();
        return new SocketIoStatusResponse(session.getSessionId(), session.getStatus(), events);
    }
}