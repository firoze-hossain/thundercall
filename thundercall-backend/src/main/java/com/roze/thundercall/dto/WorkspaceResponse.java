package com.roze.thundercall.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WorkspaceResponse {
    private Long id;
    private String name;
    private String description;
    private int collectionCount;
    private LocalDateTime createdAt;
}
