package com.roze.thundercall.ui.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FormDataField {
    private String key;
    private String type;
    private String value;
    private String fileName;
    private String fileBase64;
}