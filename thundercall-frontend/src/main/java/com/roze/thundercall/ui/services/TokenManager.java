package com.roze.thundercall.ui.services;

import com.roze.thundercall.ui.models.AuthResponse;

import java.util.prefs.Preferences;

public class TokenManager {
    private static final Preferences prefs = Preferences.userNodeForPackage(TokenManager.class);
    private static final String TOKEN_KEY = "auth_token";
    private static final String REFRESH_TOKEN_KEY = "refresh_token";
    private static final String USERNAME_KEY = "username";
    private static final String EMAIL_KEY = "email";
    private static final String USER_ID_KEY = "user_id";
    private static final String ROLE_KEY = "role";

    public static void storeTokens(AuthResponse response) {
        if (response == null) {
            return;
        }
        if (response.getToken() != null) {
            prefs.put(TOKEN_KEY, response.getToken());
            ApiClient.setToken(response.getToken());
        } else {
            System.out.println("Token is null");
        }
        prefs.putLong(USER_ID_KEY, response.getId());
        prefs.put(REFRESH_TOKEN_KEY, response.getRefreshToken());
        prefs.put(USERNAME_KEY, response.getUsername());
        prefs.put(EMAIL_KEY, response.getEmail());
        if (response.getRole() != null) {
            prefs.put(ROLE_KEY, response.getRole());
        } else {
            prefs.remove(ROLE_KEY);
        }
        // FIX: the refresh token was persisted to disk but never handed to
        // ApiClient — so ApiClient had no way to silently renew an expired
        // access token. This is what makes automatic refresh possible.
        ApiClient.setRefreshToken(response.getRefreshToken());
    }

    /** True only for an ADMIN-role account — used to hide owner-only UI
     * (like Mail Server Settings) from regular users entirely, rather
     * than showing it and letting the server reject the request. */
    public static boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(prefs.get(ROLE_KEY, null));
    }

    public static boolean isLoggedIn() {
        // FIX: this compared the STRING CONSTANT "auth_token" against itself
        // (always true) instead of checking whether a token actually
        // exists — so the app could never correctly detect "logged out".
        String currentToken = ApiClient.getToken();
        return currentToken != null && !currentToken.isEmpty();
    }

    public static String getRefreshToken() {
        return prefs.get(REFRESH_TOKEN_KEY, null);
    }

    public static String getUsername() {
        return prefs.get(USERNAME_KEY, null);
    }

    public static void clearTokens() {
        prefs.remove(TOKEN_KEY);
        prefs.remove(REFRESH_TOKEN_KEY);
        prefs.remove(USERNAME_KEY);
        prefs.remove(ROLE_KEY);
        ApiClient.setToken(null);
        ApiClient.setRefreshToken(null);
    }
}