package com.roze.thundercall.ui.services;

import com.roze.thundercall.ui.models.Workspace;

/** Tracks which workspace the app is currently "in" — your own by
 * default, or a workspace someone shared with you after switching
 * into it. sharedRole is null when you own the current workspace
 * (full access, no restrictions); "EDITOR" or "VIEWER" when it's
 * someone else's, so the rest of the UI knows whether write actions
 * should even be attempted. */
public class WorkspaceManager {
    private static Workspace currentWorkspace;
    private static String sharedRole;

    public static Workspace getCurrentWorkspace() {
        return currentWorkspace;
    }

    public static void setCurrentWorkspace(Workspace workspace) {
        currentWorkspace = workspace;
        sharedRole = null; // switching to one of your own workspaces always clears shared context
    }

    /** Use this when switching INTO a workspace shared with you,
     * instead of setCurrentWorkspace(), so the role sticks. */
    public static void setCurrentSharedWorkspace(Workspace workspace, String role) {
        currentWorkspace = workspace;
        sharedRole = role;
    }

    public static boolean hasWorkspace() {
        return currentWorkspace != null;
    }

    /** True if the current workspace belongs to someone else and you're
     * working in it via shared access, rather than your own. */
    public static boolean isViewingSharedWorkspace() {
        return sharedRole != null;
    }

    /** "EDITOR", "VIEWER", or null if you own the current workspace. */
    public static String getSharedRole() {
        return sharedRole;
    }

    public static boolean canEditCurrentWorkspace() {
        return sharedRole == null || "EDITOR".equals(sharedRole);
    }
}
