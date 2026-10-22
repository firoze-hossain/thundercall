package com.roze.thundercall.ui.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryNode {
    private boolean groupHeader;
    private String headerTitle;
    private HistoryEntry entry;

    public static HistoryNode group(String title) {
        return HistoryNode.builder()
                .groupHeader(true)
                .headerTitle(title)
                .build();
    }

    public static HistoryNode item(HistoryEntry entry) {
        return HistoryNode.builder()
                .groupHeader(false)
                .entry(entry)
                .build();
    }

    @Override
    public String toString() {
        if (groupHeader) {
            return headerTitle != null ? headerTitle : "";
        }
        if (entry != null) {
            return (entry.getMethod() != null ? entry.getMethod() : "") + " " +
                    (entry.getUrl() != null ? entry.getUrl() : "");
        }
        return "";
    }
}
