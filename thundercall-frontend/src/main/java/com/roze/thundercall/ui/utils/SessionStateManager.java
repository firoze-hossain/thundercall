package com.roze.thundercall.ui.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.roze.thundercall.ui.models.SessionData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages Postman-style session persistence and restoration across application restarts.
 * Persists open tabs, tab order, dirty/unsaved states, editor content, selected environment,
 * sidebar collection tree expanded nodes, active workspace, and active tab index.
 */
public final class SessionStateManager {
    private static final Logger LOGGER = Logger.getLogger(SessionStateManager.class.getName());
    private static final File STORE_DIR = new File(System.getProperty("user.home"), ".thundercall");
    private static final File DEFAULT_STORE_FILE = new File(STORE_DIR, "session.json");
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private SessionStateManager() {
    }

    private static File getSessionFileForUser(Long userId) {
        if (userId != null && userId > 0) {
            return new File(STORE_DIR, "session_" + userId + ".json");
        }
        return DEFAULT_STORE_FILE;
    }

    public static synchronized void saveSession(Long userId, SessionData data) {
        if (data == null) {
            return;
        }
        try {
            if (!STORE_DIR.exists()) {
                STORE_DIR.mkdirs();
            }

            File targetFile = getSessionFileForUser(userId);
            File tempFile = new File(STORE_DIR, ".session_tmp_" + System.currentTimeMillis() + ".json");

            MAPPER.writeValue(tempFile, data);

            try {
                Files.move(tempFile.toPath(), targetFile.toPath(),
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                // Fallback if atomic move isn't supported across filesystems
                Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // Also keep default session.json updated as fallback
            if (!targetFile.equals(DEFAULT_STORE_FILE)) {
                try {
                    Files.copy(targetFile.toPath(), DEFAULT_STORE_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception ignored) {
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to persist session: " + e.getMessage(), e);
        }
    }

    public static synchronized SessionData loadSession(Long userId) {
        File userFile = getSessionFileForUser(userId);
        if (userFile.exists() && userFile.length() > 0) {
            try {
                SessionData data = MAPPER.readValue(userFile, SessionData.class);
                if (data != null) {
                    return data;
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to load user session from " + userFile.getName() + ": " + e.getMessage());
            }
        }

        if (DEFAULT_STORE_FILE.exists() && DEFAULT_STORE_FILE.length() > 0) {
            try {
                SessionData data = MAPPER.readValue(DEFAULT_STORE_FILE, SessionData.class);
                if (data != null) {
                    return data;
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to load default session from " + DEFAULT_STORE_FILE.getName() + ": " + e.getMessage());
            }
        }

        return new SessionData();
    }
}
