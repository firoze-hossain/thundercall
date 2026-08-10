package com.roze.thundercall.ui.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roze.thundercall.ui.models.ScriptPackage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Local "Package Library" for reusable pre-request/test script snippets —
 * backs the Scripts editor's right-click "Save to Package Library" menu
 * (Postman-style). Stored per-machine under the user's home directory,
 * since there's no backend endpoint for this yet.
 */
public final class PackageLibraryService {

    private static final File STORE_DIR = new File(System.getProperty("user.home"), ".thundercall");
    private static final File STORE_FILE = new File(STORE_DIR, "script-packages.json");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PackageLibraryService() {
    }

    public static synchronized List<ScriptPackage> listPackages() {
        if (!STORE_FILE.exists()) {
            return new ArrayList<>();
        }
        try {
            ScriptPackage[] loaded = MAPPER.readValue(STORE_FILE, ScriptPackage[].class);
            List<ScriptPackage> result = new ArrayList<>();
            for (ScriptPackage pkg : loaded) {
                result.add(pkg);
            }
            return result;
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    /** Creates a brand-new package seeded with the given script snippet. */
    public static synchronized ScriptPackage createPackage(String name, String description, String initialScript) {
        List<ScriptPackage> packages = listPackages();
        ScriptPackage pkg = new ScriptPackage();
        pkg.setId(UUID.randomUUID().toString());
        pkg.setName(name);
        pkg.setDescription(description == null ? "" : description);
        pkg.setScript(initialScript == null ? "" : initialScript);
        packages.add(pkg);
        persist(packages);
        return pkg;
    }

    /** Appends a snippet to an already-existing package's saved script. */
    public static synchronized void appendToPackage(String packageId, String snippet) {
        List<ScriptPackage> packages = listPackages();
        for (ScriptPackage pkg : packages) {
            if (pkg.getId().equals(packageId)) {
                String existing = pkg.getScript() == null ? "" : pkg.getScript();
                pkg.setScript(existing.isEmpty() ? snippet : existing + "\n\n" + snippet);
                break;
            }
        }
        persist(packages);
    }

    private static void persist(List<ScriptPackage> packages) {
        try {
            if (!STORE_DIR.exists()) {
                Files.createDirectories(STORE_DIR.toPath());
            }
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(STORE_FILE, packages);
        } catch (IOException ignored) {
            // Best-effort local cache — losing it just means an empty library next launch.
        }
    }
}
