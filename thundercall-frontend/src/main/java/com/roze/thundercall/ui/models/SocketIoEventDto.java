package com.roze.thundercall.ui.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SocketIoEventDto {
    @Builder.Default
    private String eventName = "";
    @Builder.Default
    private boolean listen = true;
    @Builder.Default
    private String description = "";
}
