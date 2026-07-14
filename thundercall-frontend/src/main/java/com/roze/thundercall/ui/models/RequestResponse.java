package com.roze.thundercall.ui.models;

import com.roze.thundercall.ui.enums.HttpMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RequestResponse {
    private Long id;
    private String name;
    private String description;
    private HttpMethod method;
    private String url;
    private String headers;
    private String body;
    private String preRequestScript;
    private String testsScript;
    private String authType;
    private String authToken;
    private String authUsername;
    private String authPassword;
    private Long collectionId;
    private String collectionName;
    private Long folderId;
    private String folderName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}