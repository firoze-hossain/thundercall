package com.roze.thundercall.ui.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamMemberResponse {
    private Long id;
    private Long userId;
    private String username;
    private String email;
    private String role;
    private String joinedAt;
}