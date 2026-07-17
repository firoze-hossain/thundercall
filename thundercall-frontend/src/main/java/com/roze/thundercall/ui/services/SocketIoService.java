package com.roze.thundercall.ui.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.roze.thundercall.ui.models.BaseResponse;
import com.roze.thundercall.ui.models.SocketIoConnectRequest;
import com.roze.thundercall.ui.models.SocketIoEmitRequest;
import com.roze.thundercall.ui.models.SocketIoStatusResponse;

import java.io.IOException;

public class SocketIoService {
    private static final String BASE_URL = "/socketio";

    /** Throws so the caller can show the specific reason (bad URL,
     * unreachable server...) rather than a generic failure message. */
    public static SocketIoStatusResponse connect(String url, String namespace) throws IOException {
        SocketIoConnectRequest request = new SocketIoConnectRequest(url, namespace);
        BaseResponse<SocketIoStatusResponse> response = ApiClient.post(BASE_URL + "/connect", request,
                new TypeReference<BaseResponse<SocketIoStatusResponse>>() {
                });
        if (response == null || !response.isSuccess()) {
            throw new IOException("Invalid response from server");
        }
        return response.getData();
    }

    public static SocketIoStatusResponse poll(String sessionId, int since) throws IOException {
        BaseResponse<SocketIoStatusResponse> response = ApiClient.get(
                BASE_URL + "/" + sessionId + "/poll?since=" + since,
                new TypeReference<BaseResponse<SocketIoStatusResponse>>() {
                });
        if (response == null || !response.isSuccess()) {
            throw new IOException("Invalid response from server");
        }
        return response.getData();
    }

    public static void emit(String sessionId, String event, String data) throws IOException {
        SocketIoEmitRequest request = new SocketIoEmitRequest(event, data);
        ApiClient.post(BASE_URL + "/" + sessionId + "/emit", request, new TypeReference<BaseResponse<Void>>() {
        });
    }

    public static void disconnect(String sessionId) throws IOException {
        ApiClient.post(BASE_URL + "/" + sessionId + "/disconnect", null, new TypeReference<BaseResponse<Void>>() {
        });
    }

    /** Strips the "HTTP 4xx: {...}" wrapper down to the server's own
     * message where possible. */
    public static String friendlyMessage(IOException e) {
        String raw = e.getMessage();
        if (raw == null) {
            return "Something went wrong.";
        }
        int idx = raw.indexOf("\"message\":\"");
        if (idx >= 0) {
            int start = idx + "\"message\":\"".length();
            int end = raw.indexOf('"', start);
            if (end > start) {
                return raw.substring(start, end);
            }
        }
        return raw;
    }
}