package com.roze.thundercall.ui.services;

import com.roze.thundercall.ui.models.Workspace;

public class WorkspaceManager {
    private static Workspace currentWorkspace;

    public static Workspace getCurrentWorkspace() {
        return currentWorkspace;
    }

    public static void setCurrentWorkspace(Workspace workspace) {
        currentWorkspace = workspace;
    }

    public static boolean hasWorkspace() {
        return currentWorkspace != null;
    }
}
