package com.roze.thundercall.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionResponse {
    private Long id;
    private String name;
    private String description;
    private Long workspaceId;
    private String workspaceName;
    private int requestCount;
    private int folderCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<RequestResponse> requests;
    private List<FolderResponse> folderResponses;
}
