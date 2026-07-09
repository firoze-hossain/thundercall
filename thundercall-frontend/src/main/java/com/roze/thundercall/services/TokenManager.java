package com.roze.thundercall.services;

import com.roze.thundercall.models.AuthResponse;

import java.util.prefs.Preferences;

public class TokenManager {
    private static final Preferences prefs = Preferences.userNodeForPackage(TokenManager.class);
    private static final String TOKEN_KEY = "auth_token";
    private static final String REFRESH_TOKEN_KEY = "refresh_token";
    private static final String USERNAME_KEY = "username";
    private static final String EMAIL_KEY = "email";
    private static final String USER_ID_KEY = "user_id";

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


    }

    public static boolean isLoggedIn() {
        return TOKEN_KEY != null && !TOKEN_KEY.isEmpty();
    }

    public static String getUsername() {
        return prefs.get(USERNAME_KEY, null);
    }

    public static void clearTokens() {
        prefs.remove(TOKEN_KEY);
        prefs.remove(REFRESH_TOKEN_KEY);
        prefs.remove(USERNAME_KEY);
        ApiClient.setToken(null);
    }
}
