package com.roze.thundercall.ui.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.roze.thundercall.ui.models.BaseResponse;
import com.roze.thundercall.ui.models.OnlineUser;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Client side of the presence feature. While the app is running and
 * logged in, a background daemon thread POSTs a heartbeat every 25
 * seconds; the server considers a user online if it heard from them in
 * the last 70 seconds (so one dropped request doesn't flicker anyone
 * offline). getOnlineUsers() returns the currently-online people related
 * to this account — teammates and workspace-sharing connections.
 */
public class PresenceService {
    private static final long HEARTBEAT_SECONDS = 25;
    private static ScheduledExecutorService heartbeat;

    /** Idempotent — safe to call again after a re-login. */
    public static synchronized void startHeartbeat() {
        if (heartbeat != null && !heartbeat.isShutdown()) {
            return;
        }
        heartbeat = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "presence-heartbeat");
            t.setDaemon(true);
            return t;
        });
        heartbeat.scheduleAtFixedRate(PresenceService::sendHeartbeat,
                0, HEARTBEAT_SECONDS, TimeUnit.SECONDS);
    }

    /** Called on logout so a signed-out client stops reporting itself online. */
    public static synchronized void stopHeartbeat() {
        if (heartbeat != null) {
            heartbeat.shutdownNow();
            heartbeat = null;
        }
    }

    private static void sendHeartbeat() {
        if (!TokenManager.isLoggedIn()) {
            return;
        }
        try {
            ApiClient.post("/presence/heartbeat", null,
                    new TypeReference<BaseResponse<Void>>() {
                    });
        } catch (IOException e) {
            // A missed heartbeat just means we briefly show offline —
            // never worth interrupting the user over.
            System.err.println("Presence heartbeat failed: " + e.getMessage());
        }
    }

    /** Blocking — call from a background thread, not the FX thread. */
    public static List<OnlineUser> getOnlineUsers() {
        try {
            BaseResponse<List<OnlineUser>> response = ApiClient.get("/presence/online",
                    new TypeReference<BaseResponse<List<OnlineUser>>>() {
                    });
            if (response != null && response.isSuccess() && response.getData() != null) {
                return response.getData();
            }
        } catch (IOException e) {
            System.err.println("Failed to load online users: " + e.getMessage());
        }
        return Collections.emptyList();
    }
}
