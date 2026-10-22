package com.roze.thundercall.ui.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.roze.thundercall.ui.models.HistoryEntry;
import com.roze.thundercall.ui.models.RequestHistoryResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;

public final class HistoryStorageService {
    private static final File STORE_DIR = new File(System.getProperty("user.home"), ".thundercall");
    private static final File STORE_FILE = new File(STORE_DIR, "history.json");
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(SerializationFeature.INDENT_OUTPUT);
    private static final int MAX_ENTRIES = 1000;

    private HistoryStorageService() {
    }

    public static synchronized List<HistoryEntry> loadLocalHistory() {
        File fileToRead = STORE_FILE;
        if (!fileToRead.exists() || fileToRead.length() <= 3) {
            File tmp = new File(STORE_DIR, "history.json.tmp");
            if (tmp.exists() && tmp.length() > 3) {
                fileToRead = tmp;
            }
        }
        if (!fileToRead.exists()) {
            return new ArrayList<>();
        }
        try {
            List<HistoryEntry> list = MAPPER.readValue(fileToRead, new TypeReference<List<HistoryEntry>>() {});
            return list != null ? new ArrayList<>(list) : new ArrayList<>();
        } catch (Exception e) {
            // If primary read fails (e.g., malformed or interrupted write), try fallback to tmp
            File tmp = new File(STORE_DIR, "history.json.tmp");
            if (!fileToRead.equals(tmp) && tmp.exists() && tmp.length() > 3) {
                try {
                    List<HistoryEntry> list = MAPPER.readValue(tmp, new TypeReference<List<HistoryEntry>>() {});
                    if (list != null && !list.isEmpty()) {
                        saveLocalHistory(list);
                        return new ArrayList<>(list);
                    }
                } catch (Exception ignored) {
                }
            }
            return new ArrayList<>();
        }
    }

    public static synchronized void saveLocalHistory(List<HistoryEntry> entries) {
        if (!STORE_DIR.exists()) {
            STORE_DIR.mkdirs();
        }
        try {
            String json = MAPPER.writeValueAsString(entries != null ? entries : Collections.emptyList());
            File tmp = new File(STORE_DIR, "history.json.tmp");
            Files.writeString(tmp.toPath(), json);
            try {
                Files.move(tmp.toPath(), STORE_FILE.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception ex) {
                // Fallback without ATOMIC_MOVE
                Files.move(tmp.toPath(), STORE_FILE.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            // Direct write fallback
            try {
                String json = MAPPER.writeValueAsString(entries != null ? entries : Collections.emptyList());
                Files.writeString(STORE_FILE.toPath(), json);
            } catch (Exception ignored) {
            }
        }
    }

    public static synchronized void addEntry(HistoryEntry entry) {
        if (entry == null) return;
        if (entry.getLocalId() == null || entry.getLocalId().isBlank()) {
            entry.setLocalId(UUID.randomUUID().toString());
        }
        if (entry.getTimestamp() == null || entry.getTimestamp().isBlank()) {
            entry.setTimestamp(LocalDateTime.now().toString());
        }

        List<HistoryEntry> current = loadLocalHistory();
        // Remove existing duplicate with identical method + url + recent timestamp if any
        current.removeIf(e -> Objects.equals(e.getLocalId(), entry.getLocalId())
                || (Objects.equals(e.getServerId(), entry.getServerId()) && entry.getServerId() != null));

        current.add(0, entry);
        if (current.size() > MAX_ENTRIES) {
            current = new ArrayList<>(current.subList(0, MAX_ENTRIES));
        }
        saveLocalHistory(current);
    }

    public static synchronized void removeEntry(HistoryEntry entry) {
        if (entry == null) return;
        List<HistoryEntry> current = loadLocalHistory();
        current.removeIf(e -> (entry.getLocalId() != null && Objects.equals(e.getLocalId(), entry.getLocalId()))
                || (entry.getServerId() != null && Objects.equals(e.getServerId(), entry.getServerId())));
        saveLocalHistory(current);
    }

    public static synchronized void clearAll() {
        saveLocalHistory(new ArrayList<>());
    }

    public static synchronized List<HistoryEntry> mergeWithServer(List<RequestHistoryResponse> serverHistory) {
        List<HistoryEntry> local = loadLocalHistory();
        if (serverHistory == null || serverHistory.isEmpty()) {
            return local;
        }

        Map<Long, HistoryEntry> serverMap = new LinkedHashMap<>();
        for (RequestHistoryResponse s : serverHistory) {
            if (s.getId() == null) continue;
            HistoryEntry e = HistoryEntry.builder()
                    .serverId(s.getId())
                    .requestId(s.getRequestId())
                    .name(s.getRequestName())
                    .url(s.getUrl())
                    .method(s.getMethod() != null ? s.getMethod() : "GET")
                    .headers(s.getRequestHeaders())
                    .body(s.getRequestBody())
                    .statusCode(s.getStatusCode())
                    .duration(s.getDuration())
                    .response(s.getResponse())
                    .responseHeaders(s.getResponseHeaders())
                    .timestamp(s.getTimestamp() != null ? s.getTimestamp().toString() : LocalDateTime.now().toString())
                    .build();
            serverMap.put(s.getId(), e);
        }

        // Merge: keep local entries (preserves custom local headers/body if server omitted),
        // update matches, and insert new server entries
        List<HistoryEntry> merged = new ArrayList<>();
        Set<Long> matchedServerIds = new HashSet<>();

        for (HistoryEntry loc : local) {
            if (loc.getServerId() != null && serverMap.containsKey(loc.getServerId())) {
                HistoryEntry srv = serverMap.get(loc.getServerId());
                // Merge fields
                if (loc.getUrl() == null || loc.getUrl().isBlank()) loc.setUrl(srv.getUrl());
                if (loc.getMethod() == null || loc.getMethod().isBlank()) loc.setMethod(srv.getMethod());
                if (loc.getHeaders() == null || loc.getHeaders().isBlank()) loc.setHeaders(srv.getHeaders());
                if (loc.getBody() == null || loc.getBody().isBlank()) loc.setBody(srv.getBody());
                if (loc.getStatusCode() == null) loc.setStatusCode(srv.getStatusCode());
                if (loc.getDuration() == null) loc.setDuration(srv.getDuration());
                matchedServerIds.add(loc.getServerId());
            }
            merged.add(loc);
        }

        for (Map.Entry<Long, HistoryEntry> entry : serverMap.entrySet()) {
            if (!matchedServerIds.contains(entry.getKey())) {
                HistoryEntry srv = entry.getValue();
                srv.setLocalId(UUID.randomUUID().toString());
                merged.add(srv);
            }
        }

        // Sort descending by timestamp
        merged.sort((a, b) -> {
            String ta = a.getTimestamp() != null ? a.getTimestamp() : "";
            String tb = b.getTimestamp() != null ? b.getTimestamp() : "";
            return tb.compareTo(ta);
        });

        if (merged.size() > MAX_ENTRIES) {
            merged = new ArrayList<>(merged.subList(0, MAX_ENTRIES));
        }

        saveLocalHistory(merged);
        return merged;
    }
}
