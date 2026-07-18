package com.roze.thundercall.ui.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InviteToWorkspaceRequest {
    private String email;
    private String role; // "EDITOR" or "VIEWER"
    private List<Long> environmentIds;
}
