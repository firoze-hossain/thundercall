package com.roze.thundercall.ui.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HistoryEntry {
    private String localId;
    private Long serverId;
    private Long requestId;
    private String name;
    private String url;
    private String method;
    private String headers;
    private String body;
    private Integer statusCode;
    private Long duration;
    private String response;
    private String responseHeaders;
    private String timestamp; // ISO format e.g. 2026-10-22T23:14:00

    @JsonIgnore
    public LocalDate getDate() {
        if (timestamp == null || timestamp.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDateTime.parse(timestamp).toLocalDate();
        } catch (Exception e) {
            try {
                return LocalDate.parse(timestamp);
            } catch (Exception ex) {
                return LocalDate.now();
            }
        }
    }

    @JsonIgnore
    public String getDateGroupHeader() {
        LocalDate date = getDate();
        LocalDate today = LocalDate.now();
        if (date.isEqual(today)) {
            return "Today";
        } else if (date.isEqual(today.minusDays(1))) {
            return "Yesterday";
        } else if (date.getYear() == today.getYear()) {
            return date.format(DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH));
        } else {
            return date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH));
        }
    }
}
