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
public class KeyValuePairDto {
    @Builder.Default
    private boolean enabled = true;
    @Builder.Default
    private String key = "";
    @Builder.Default
    private String value = "";
    @Builder.Default
    private String description = "";
    @Builder.Default
    private boolean placeholder = false;
    @Builder.Default
    private String type = "Text";
    @Builder.Default
    private String filePath = "";
    @Builder.Default
    private String fileName = "";
}
