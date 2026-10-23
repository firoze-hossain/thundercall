package com.roze.thundercall.ui.services;

import com.roze.thundercall.ui.Main;
import com.roze.thundercall.ui.models.AuthResponse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Universal Desktop Browser SSO Authentication Service (Postman & VS Code Pattern).
 * Starts an ephemeral local HTTP server on 127.0.0.1:0 to safely receive the authentication
 * callback from the browser after RozeHub login.
 */
public class DesktopBrowserAuthService {

    private static HttpServer activeServer;
    private static String expectedState;

    public static synchronized void startBrowserAuth(Consumer<String> statusConsumer,
                                                     Runnable onSuccess,
                                                     Consumer<String> onError) {
        cancelActiveAuth();

        try {
            // Bind to ephemeral port 0 (OS chooses an open port)
            activeServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            int port = activeServer.getAddress().getPort();
            expectedState = UUID.randomUUID().toString();

            activeServer.createContext("/callback", exchange -> {
                try {
                    Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
                    String returnedState = query.get("state");
                    String token = query.get("token");
                    String refreshToken = query.get("refreshToken");
                    String username = query.get("username");
                    String email = query.get("email");
                    String idStr = query.get("id");
                    Long id = idStr != null ? parseLongSafely(idStr) : 1L;

                    if (returnedState == null || !returnedState.equals(expectedState)) {
                        sendHtmlResponse(exchange, 400, "Invalid authorization state. Please try again.");
                        if (onError != null) {
                            Platform.runLater(() -> onError.accept("Authorization state mismatch."));
                        }
                        return;
                    }

                    // Success HTML response shown to the user in their browser
                    String successHtml = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta charset="utf-8">
                                <title>Connected to ThunderCall</title>
                                <style>
                                    body { background: #090d16; color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; }
                                    .card { background: #111827; border: 1px solid #1f2937; border-radius: 16px; padding: 40px 32px; text-align: center; max-width: 420px; box-shadow: 0 20px 35px rgba(0,0,0,0.6); }
                                    .icon { width: 56px; height: 56px; border-radius: 50%; background: rgba(16, 185, 129, 0.15); border: 2px solid #10b981; color: #10b981; font-size: 26px; display: flex; align-items: center; justify-content: center; margin: 0 auto 18px; }
                                    h1 { font-size: 20px; font-weight: 700; margin: 0 0 10px; color: #fff; }
                                    p { font-size: 13px; color: #94a3b8; line-height: 1.5; margin: 0 0 24px; }
                                    .hint { font-size: 12px; color: #64748b; }
                                </style>
                            </head>
                            <body>
                                <div class="card">
                                    <div class="icon">✓</div>
                                    <h1>Connected to ThunderCall!</h1>
                                    <p>Your RozeHub single account is now active in ThunderCall. You can close this tab and return to the application.</p>
                                    <div class="hint">Window will close automatically...</div>
                                </div>
                                <script>setTimeout(() => window.close(), 2500);</script>
                            </body>
                            </html>
                            """;

                    sendHtmlResponse(exchange, 200, successHtml);

                    // Process authentication with ThunderCall backend
                    new Thread(() -> {
                        try {
                            AuthResponse response;
                            try {
                                response = ApiClient.ssoLogin(email, username, token, "rozehub");
                            } catch (Exception ex) {
                                throw new IOException("Backend server (port 9084) is not reachable: " + ex.getMessage(), ex);
                            }

                            AuthResponse finalResponse = response;
                            Platform.runLater(() -> {
                                TokenManager.startNewSession(finalResponse);
                                if (onSuccess != null) {
                                    onSuccess.run();
                                }
                            });
                        } catch (Exception ex) {
                            if (onError != null) {
                                Platform.runLater(() -> onError.accept("Sign-in failed: " + ex.getMessage()));
                            }
                        }
                    }).start();

                } finally {
                    // Stop loopback server shortly after delivering the response
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            cancelActiveAuth();
                        }
                    }, 1200);
                }
            });

            activeServer.setExecutor(Executors.newSingleThreadExecutor());
            activeServer.start();

            if (statusConsumer != null) {
                statusConsumer.accept("Waiting for browser authorization...");
            }

            // Launch default browser to RozeHub desktop SSO endpoint
            String rozeHubUrl = System.getProperty("rozehub.url", "http://localhost:8000");
            String authUrl = String.format("%s/auth/desktop?app=thundercall&port=%d&state=%s",
                    rozeHubUrl, port, expectedState);

            Main.getAppHostServices().showDocument(authUrl);

        } catch (Exception ex) {
            cancelActiveAuth();
            if (onError != null) {
                onError.accept("Could not launch browser login: " + ex.getMessage());
            }
        }
    }

    public static synchronized void cancelActiveAuth() {
        if (activeServer != null) {
            try {
                activeServer.stop(0);
            } catch (Exception ignored) {}
            activeServer = null;
        }
    }

    private static void sendHtmlResponse(HttpExchange exchange, int statusCode, String html) {
        try {
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (Exception ignored) {}
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isBlank()) return result;
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2) {
                result.put(pair[0], URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
            }
        }
        return result;
    }

    private static Long parseLongSafely(String str) {
        try {
            return Long.parseLong(str);
        } catch (Exception e) {
            return 1L;
        }
    }
}
