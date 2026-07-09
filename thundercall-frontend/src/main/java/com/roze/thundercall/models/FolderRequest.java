package com.roze.thundercall.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
//@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FolderRequest {
    private  String name;
    private  String description;
    private  Long collectionId;

    public FolderRequest(String name, String description, Long collectionId) {
        this.name = name;
        this.description = description;
        this.collectionId = collectionId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getCollectionId() {
        return collectionId;
    }
}