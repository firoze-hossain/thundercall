package com.roze.thundercall.ui.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamResponse {
    private Long id;
    private String name;
    private String description;
    private String ownerUsername;
    private String ownerEmail;
    private long memberCount;
    // "OWNER" / "ADMIN" / "MEMBER" — kept as a plain string (not a frontend
    // enum) since it's only ever displayed or compared against literals.
    private String myRole;
    private String createdAt;
}