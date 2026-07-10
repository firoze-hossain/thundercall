package com.roze.thundercall.ui.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.prefs.Preferences;

/**
 * "Remember me" storage for the login screen.
 *
 * Stores the username and (Base64-obfuscated) password in the OS user
 * preferences, so the login form is pre-filled on the next launch.
 *
 * NOTE: Base64 is obfuscation, not encryption — fine for a local desktop
 * tool, but if this app ever ships broadly, switch to the OS keychain
 * (libsecret / Windows Credential Manager / macOS Keychain).
 */
public final class CredentialStore {

    private static final String KEY_USER = "thundercall.remember.user";
    private static final String KEY_PASS = "thundercall.remember.pass";

    private CredentialStore() {
    }

    public static void save(String username, String password) {
        Preferences p = prefs();
        p.put(KEY_USER, username == null ? "" : username);
        p.put(KEY_PASS, password == null ? "" : Base64.getEncoder()
                .encodeToString(password.getBytes(StandardCharsets.UTF_8)));
    }

    public static void clear() {
        Preferences p = prefs();
        p.remove(KEY_USER);
        p.remove(KEY_PASS);
    }

    public static boolean hasSaved() {
        return !prefs().get(KEY_USER, "").isEmpty();
    }

    public static String savedUsername() {
        return prefs().get(KEY_USER, "");
    }

    public static String savedPassword() {
        String encoded = prefs().get(KEY_PASS, "");
        if (encoded.isEmpty()) {
            return "";
        }
        try {
            return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    private static Preferences prefs() {
        return Preferences.userNodeForPackage(CredentialStore.class);
    }
}