package com.roze.thundercall.ui.utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * "Remember me" storage — now supports MULTIPLE saved accounts, so the
 * login page can suggest them like a browser: click the username field,
 * pick an account, both fields fill in.
 *
 * NOTE: passwords are Base64-obfuscated in OS user preferences — fine for
 * a local desktop tool; use the OS keychain for public distribution.
 */
public final class CredentialStore {

    private static final String KEY_ACCOUNTS = "thundercall.remember.accounts";
    private static final String KEY_PASS_PREFIX = "thundercall.remember.pass.";
    private static final String KEY_LAST = "thundercall.remember.last";

    private CredentialStore() {
    }

    /** Saves/updates one account and marks it as the most recent. */
    public static void save(String username, String password) {
        if (username == null || username.isBlank()) {
            return;
        }
        Preferences p = prefs();
        List<String> accounts = savedUsernames();
        if (!accounts.contains(username)) {
            accounts.add(username);
            p.put(KEY_ACCOUNTS, String.join("\u0001", accounts));
        }
        p.put(KEY_PASS_PREFIX + username, encode(password == null ? "" : password));
        p.put(KEY_LAST, username);
    }

    /** Removes one saved account. */
    public static void remove(String username) {
        Preferences p = prefs();
        List<String> accounts = savedUsernames();
        accounts.remove(username);
        p.put(KEY_ACCOUNTS, String.join("\u0001", accounts));
        p.remove(KEY_PASS_PREFIX + username);
        if (username.equals(p.get(KEY_LAST, ""))) {
            p.remove(KEY_LAST);
        }
    }

    public static void clear() {
        Preferences p = prefs();
        for (String username : savedUsernames()) {
            p.remove(KEY_PASS_PREFIX + username);
        }
        p.remove(KEY_ACCOUNTS);
        p.remove(KEY_LAST);
    }

    public static boolean hasSaved() {
        return !savedUsernames().isEmpty();
    }

    public static List<String> savedUsernames() {
        String raw = prefs().get(KEY_ACCOUNTS, "");
        if (raw.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(raw.split("\u0001")));
    }

    /** The most recently saved/used account, or empty string. */
    public static String savedUsername() {
        String last = prefs().get(KEY_LAST, "");
        if (!last.isEmpty()) {
            return last;
        }
        List<String> accounts = savedUsernames();
        return accounts.isEmpty() ? "" : accounts.get(accounts.size() - 1);
    }

    public static String savedPassword() {
        return passwordFor(savedUsername());
    }

    public static String passwordFor(String username) {
        if (username == null || username.isEmpty()) {
            return "";
        }
        String encoded = prefs().get(KEY_PASS_PREFIX + username, "");
        if (encoded.isEmpty()) {
            return "";
        }
        try {
            return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static Preferences prefs() {
        return Preferences.userNodeForPackage(CredentialStore.class);
    }
}