package com.roze.thundercall.ui.services;

import com.roze.thundercall.ui.models.HistoryEntry;
import com.roze.thundercall.ui.models.RequestHistoryResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class HistoryStorageServiceTest {

    @Test
    public void testHistoryLoadAndSave() {
        List<HistoryEntry> initial = HistoryStorageService.loadLocalHistory();
        assertNotNull(initial);

        HistoryEntry entry = HistoryEntry.builder()
                .localId(UUID.randomUUID().toString())
                .name("Test Login")
                .url("http://localhost:8090/api/v1/test")
                .method("POST")
                .statusCode(200)
                .duration(42L)
                .timestamp(LocalDateTime.now().toString())
                .build();

        HistoryStorageService.addEntry(entry);

        List<HistoryEntry> reloaded = HistoryStorageService.loadLocalHistory();
        assertFalse(reloaded.isEmpty());
        assertTrue(reloaded.stream().anyMatch(e -> "http://localhost:8090/api/v1/test".equals(e.getUrl())));

        // Test Date formatting
        assertEquals("Today", entry.getDateGroupHeader());

        // Clean up test entry
        HistoryStorageService.removeEntry(entry);
    }
}
