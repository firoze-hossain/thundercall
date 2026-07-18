package com.roze.thundercall.ui.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberRoleEntry {
    private Long userId;
    private String role; // "EDITOR" or "VIEWER"
    // Explicit opt-in — which environments this member should see.
    // Empty/null means none, not "all".
    private List<Long> environmentIds;
}
