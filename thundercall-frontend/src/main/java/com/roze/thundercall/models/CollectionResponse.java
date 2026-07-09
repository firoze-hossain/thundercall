package com.roze.thundercall.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CollectionResponse {
    private Long id;
    private String name;
    private String description;
    private String workspaceId;
    private String workspaceName;
    private int requestCount;
    private int folderCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<RequestResponse> requestResponses;
    private List<FolderResponse> folderResponses;
}
