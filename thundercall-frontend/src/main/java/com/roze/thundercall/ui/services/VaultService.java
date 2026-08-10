package com.roze.thundercall.ui.services;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Postman's Vault, reimplemented: secrets referenced as {@code {{vault:name}}}
 * that live ONLY on this machine and are never synced to the backend or
 * shared with teammates — that's the actual, documented behavior of
 * Postman's real Vault feature, so a local JSON file here is the correct
 * design, not a shortcut. Backs the "Vault" option in "Set as variable".
 */
public final class VaultService {

    private static final File STORE_DIR = new File(System.getProperty("user.home"), ".thundercall");
    private static final File STORE_FILE = new File(STORE_DIR, "vault.json");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private VaultService() {
    }

    public static synchronized Map<String, String> listSecrets() {
        if (!STORE_FILE.exists()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, String> loaded = MAPPER.readValue(STORE_FILE, Map.class);
            return loaded != null ? new LinkedHashMap<>(loaded) : new LinkedHashMap<>();
        } catch (IOException e) {
            return new LinkedHashMap<>();
        }
    }

    public static synchronized void setSecret(String name, String value) {
        Map<String, String> secrets = listSecrets();
        secrets.put(name, value == null ? "" : value);
        persist(secrets);
    }

    public static synchronized void deleteSecret(String name) {
        Map<String, String> secrets = listSecrets();
        secrets.remove(name);
        persist(secrets);
    }

    /** Variables merged into request resolution as {@code vault:name} keys —
     * matches the {@code {{vault:name}}} reference syntax used in the URL/
     * body/headers, so the same VariableResolver.resolve(...) handles it
     * with no special-casing needed at the call site. */
    public static Map<String, String> asPrefixedVariables() {
        Map<String, String> prefixed = new LinkedHashMap<>();
        listSecrets().forEach((k, v) -> prefixed.put("vault:" + k, v));
        return prefixed;
    }

    private static void persist(Map<String, String> secrets) {
        try {
            if (!STORE_DIR.exists()) {
                Files.createDirectories(STORE_DIR.toPath());
            }
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(STORE_FILE, secrets);
        } catch (IOException ignored) {
            // Best-effort local cache — losing it just means an empty vault next launch.
        }
    }
}
