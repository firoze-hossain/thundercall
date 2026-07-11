package com.roze.thundercall.ui.models;

import com.roze.thundercall.ui.enums.HttpMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiRequest {
    private String name;
    private String description;
    private HttpMethod method;
    private String url;
    private String headers;
    private String body;
    private Long collectionId;
    private Long folderId;
    private String preRequestScript;
    private String testsScript;
}