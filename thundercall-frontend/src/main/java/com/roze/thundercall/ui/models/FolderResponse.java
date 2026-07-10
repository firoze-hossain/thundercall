package com.roze.thundercall.ui.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FolderResponse {
    private Long id;
    private String name;
    private String description;
    private Long collectionId;
    private String collectionName;
    private int requestCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}