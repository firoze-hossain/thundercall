package com.roze.thundercall.ui.services;

import com.roze.thundercall.ui.Main;
import com.roze.thundercall.ui.models.AuthResponse;
import com.roze.thundercall.ui.utils.AlertUtils;
import javafx.application.Platform;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

/**
 * Manages user authentication tokens and the 7-day persistent login session.
 *
 * Session Behavior:
 * - When a user logs in, a persistent 7-day session is established.
 * - During app restarts within the 7-day window, the user is kept logged in
 *   and lands directly on their workspace without re-entering credentials.
 * - An active background monitor tracks the remaining session duration.
 * - If 7 days elapse without manual logout, the session automatically expires,
 *   all tokens are wiped, and the user is cleanly returned to the login screen
 *   with an informative message.
 * - Manual logout immediately clears the session locally and on the server.
 */
public class TokenManager {
    private static final Preferences prefs = Preferences.userNodeForPackage(TokenManager.class);

    private static final String TOKEN_KEY = "auth_token";
    private static final String REFRESH_TOKEN_KEY = "refresh_token";
    private static final String USERNAME_KEY = "username";
    private static final String EMAIL_KEY = "email";
    private static final String USER_ID_KEY = "user_id";
    private static final String ROLE_KEY = "role";

    public static final long SESSION_DURATION_MS = 7L * 24L * 60L * 60L * 1000L; // 7 days (604,800,000 ms)
    private static final String SESSION_CREATED_AT_KEY = "session_created_at";
    private static final String SESSION_EXPIRES_AT_KEY = "session_expires_at";
    private static final String SESSION_EXPIRED_ALERT_KEY = "session_expired_alert";

    private static ScheduledExecutorService sessionMonitor;

    /**
     * Starts a brand new 7-day session upon successful user authentication.
     */
    public static void startNewSession(AuthResponse response) {
        storeTokens(response, true);
    }

    /**
     * Stores/updates tokens. When called as a silent token refresh,
     * it preserves the existing 7-day session window.
     */
    public static void storeTokens(AuthResponse response) {
        storeTokens(response, false);
    }

    public static void storeTokens(AuthResponse response, boolean isNewLogin) {
        if (response == null) {
            return;
        }

        long now = System.currentTimeMillis();
        long existingExpiresAt = prefs.getLong(SESSION_EXPIRES_AT_KEY, 0L);

        if (isNewLogin || existingExpiresAt <= 0) {
            prefs.putLong(SESSION_CREATED_AT_KEY, now);
            prefs.putLong(SESSION_EXPIRES_AT_KEY, now + SESSION_DURATION_MS);
            prefs.remove(SESSION_EXPIRED_ALERT_KEY);
        }

        if (response.getToken() != null) {
            prefs.put(TOKEN_KEY, response.getToken());
            ApiClient.setToken(response.getToken());
        }

        if (response.getRefreshToken() != null) {
            prefs.put(REFRESH_TOKEN_KEY, response.getRefreshToken());
            ApiClient.setRefreshToken(response.getRefreshToken());
        }

        prefs.putLong(USER_ID_KEY, response.getId());
        if (response.getUsername() != null) {
            prefs.put(USERNAME_KEY, response.getUsername());
        }
        if (response.getEmail() != null) {
            prefs.put(EMAIL_KEY, response.getEmail());
        }
        if (response.getRole() != null) {
            prefs.put(ROLE_KEY, response.getRole());
        } else {
            prefs.remove(ROLE_KEY);
        }

        startSessionMonitor();
    }

    /**
     * Checks if the 7-day session has elapsed.
     */
    public static boolean isSessionExpired() {
        long expiresAt = prefs.getLong(SESSION_EXPIRES_AT_KEY, 0L);
        if (expiresAt <= 0) {
            // Check if legacy tokens exist without an explicit expiration
            String token = prefs.get(TOKEN_KEY, null);
            if (token != null && !token.isBlank()) {
                long now = System.currentTimeMillis();
                prefs.putLong(SESSION_CREATED_AT_KEY, now);
                prefs.putLong(SESSION_EXPIRES_AT_KEY, now + SESSION_DURATION_MS);
                return false;
            }
            return true;
        }
        return System.currentTimeMillis() >= expiresAt;
    }

    /**
     * Returns true if there is a valid, unexpired 7-day session.
     */
    public static boolean hasValidSession() {
        String token = prefs.get(TOKEN_KEY, null);
        String refreshToken = prefs.get(REFRESH_TOKEN_KEY, null);
        if (token == null || token.isBlank() || refreshToken == null || refreshToken.isBlank()) {
            return false;
        }
        return !isSessionExpired();
    }

    /**
     * Restores an active session into ApiClient and starts the background monitor.
     * If the session has expired while the app was closed, it cleans up and flags the expiration.
     */
    public static boolean restoreSession() {
        String refreshToken = prefs.get(REFRESH_TOKEN_KEY, null);
        if (refreshToken != null && !refreshToken.isBlank() && isSessionExpired()) {
            System.out.println("[TokenManager] 7-day session expired while application was closed.");
            prefs.putBoolean(SESSION_EXPIRED_ALERT_KEY, true);
            clearTokens();
            return false;
        }

        if (!hasValidSession()) {
            return false;
        }

        String token = prefs.get(TOKEN_KEY, null);
        ApiClient.setToken(token);
        ApiClient.setRefreshToken(refreshToken);
        startSessionMonitor();
        return true;
    }

    /**
     * True if the session was automatically expired (used to display an alert on login view).
     */
    public static boolean wasSessionExpired() {
        return prefs.getBoolean(SESSION_EXPIRED_ALERT_KEY, false);
    }

    public static void clearSessionExpiredAlert() {
        prefs.remove(SESSION_EXPIRED_ALERT_KEY);
    }

    /**
     * Remaining milliseconds in the 7-day session.
     */
    public static long getRemainingSessionTimeMillis() {
        long expiresAt = prefs.getLong(SESSION_EXPIRES_AT_KEY, 0L);
        return Math.max(0L, expiresAt - System.currentTimeMillis());
    }

    /**
     * Human-readable remaining session time string (e.g. "6 days, 21 hrs left").
     */
    public static String getSessionTimeRemainingHumanReadable() {
        long remaining = getRemainingSessionTimeMillis();
        if (remaining <= 0) {
            return "Expired";
        }
        long days = TimeUnit.MILLISECONDS.toDays(remaining);
        long hours = TimeUnit.MILLISECONDS.toHours(remaining) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60;
        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + (hours > 0 ? ", " + hours + " hr" + (hours > 1 ? "s" : "") : "") + " left";
        } else if (hours > 0) {
            return hours + " hr" + (hours > 1 ? "s" : "") + (minutes > 0 ? ", " + minutes + " min" : "") + " left";
        } else {
            return Math.max(1, minutes) + " min left";
        }
    }

    /** True only for an ADMIN-role account. */
    public static boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(prefs.get(ROLE_KEY, null));
    }

    public static boolean isLoggedIn() {
        String currentToken = ApiClient.getToken();
        return currentToken != null && !currentToken.isEmpty() && !isSessionExpired();
    }

    public static String getRefreshToken() {
        return prefs.get(REFRESH_TOKEN_KEY, null);
    }

    public static String getUsername() {
        return prefs.get(USERNAME_KEY, null);
    }

    public static Long getUserId() {
        long id = prefs.getLong(USER_ID_KEY, -1L);
        return id == -1L ? null : id;
    }

    /**
     * Real-time background session monitor that checks for 7-day expiration
     * every 30 seconds while the application is active.
     */
    public static synchronized void startSessionMonitor() {
        stopSessionMonitor();
        sessionMonitor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "thundercall-session-monitor");
            t.setDaemon(true);
            return t;
        });
        sessionMonitor.scheduleAtFixedRate(TokenManager::checkSessionExpiry, 30, 30, TimeUnit.SECONDS);
    }

    public static synchronized void stopSessionMonitor() {
        if (sessionMonitor != null) {
            sessionMonitor.shutdownNow();
            sessionMonitor = null;
        }
    }

    private static void checkSessionExpiry() {
        if (ApiClient.getToken() != null && isSessionExpired()) {
            System.out.println("[TokenManager] 7-day session expired during active app usage. Auto-logging out.");
            prefs.putBoolean(SESSION_EXPIRED_ALERT_KEY, true);
            stopSessionMonitor();
            PresenceService.stopHeartbeat();
            ApiClient.logout();
            clearTokens();
            Platform.runLater(() -> {
                AlertUtils.showInfo("Your 7-day session has expired. Please log in again.");
                Main.showLoginView();
            });
        }
    }

    /**
     * Clears all session keys, stops monitors, and clears ApiClient memory tokens.
     */
    public static void clearTokens() {
        stopSessionMonitor();
        prefs.remove(TOKEN_KEY);
        prefs.remove(REFRESH_TOKEN_KEY);
        prefs.remove(USERNAME_KEY);
        prefs.remove(EMAIL_KEY);
        prefs.remove(USER_ID_KEY);
        prefs.remove(ROLE_KEY);
        prefs.remove(SESSION_CREATED_AT_KEY);
        prefs.remove(SESSION_EXPIRES_AT_KEY);
        ApiClient.setToken(null);
        ApiClient.setRefreshToken(null);
    }
}