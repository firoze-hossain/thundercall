package com.roze.thundercall.ui.update;

import java.util.Map;

/** Immutable description of an application update returned by RozeHub. */
public record AppUpdate(
        String product,
        String currentVersion,
        String latestVersion,
        boolean available,
        boolean mandatory,
        String minimumSupportedVersion,
        ReleaseArtifact updateArtifact,
        ReleaseArtifact installerArtifact,
        String releaseNotes
) {
    public record ReleaseArtifact(
            long id,
            String purpose,
            String packageType,
            String platform,
            String architecture,
            String fileName,
            long fileSize,
            String sha256,
            String downloadUrl
    ) {}

    public ReleaseArtifact preferredUpdateArtifact() {
        return updateArtifact != null ? updateArtifact : installerArtifact;
    }
}
