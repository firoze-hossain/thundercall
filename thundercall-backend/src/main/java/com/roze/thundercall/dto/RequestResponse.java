package com.roze.thundercall.dto;

import com.roze.thundercall.enums.HttpMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestResponse {
    private Long id;
    private String name;
    private String description;
    private HttpMethod method;
    private String url;
    private String headers;
    private String body;
    private Long collectionId;
    private String collectionName;
    private Long folderId;
    private String folderName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
