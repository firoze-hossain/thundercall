package com.roze.thundercall.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Workspace {
    private Long id;
    private String name;
    private String description;
    private int collectionCount;
    private LocalDateTime createdAt;
}
