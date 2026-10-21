package com.roze.thundercall.ui.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TabStateDto {
    @Builder.Default
    private String name = "Untitled Request";
    private Long requestId;
    private Long collectionId;
    @Builder.Default
    private String method = "GET";
    @Builder.Default
    private String url = "";
    @Builder.Default
    private String body = "";
    @Builder.Default
    private String bodyType = "None";
    @Builder.Default
    private String graphqlQuery = "";
    @Builder.Default
    private String graphqlVariables = "";
    @Builder.Default
    private String preScript = "";
    @Builder.Default
    private String testsScript = "";
    @Builder.Default
    private String authType = "No Auth";
    @Builder.Default
    private String authToken = "";
    @Builder.Default
    private String authUsername = "";
    @Builder.Default
    private String authPassword = "";
    @Builder.Default
    private List<KeyValuePairDto> params = new ArrayList<>();
    @Builder.Default
    private List<KeyValuePairDto> headers = new ArrayList<>();
    @Builder.Default
    private List<KeyValuePairDto> formData = new ArrayList<>();
    @Builder.Default
    private List<KeyValuePairDto> urlEncoded = new ArrayList<>();
    @Builder.Default
    private String socketIoNamespace = "";
    @Builder.Default
    private String socketIoEventName = "";
    @Builder.Default
    private String socketIoMessageBody = "";
    @Builder.Default
    private List<SocketIoEventDto> socketIoEvents = new ArrayList<>();
    @Builder.Default
    private boolean dirty = false;
}
