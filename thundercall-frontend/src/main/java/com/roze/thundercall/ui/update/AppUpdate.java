package com.roze.thundercall.ui.update;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Locale;

/**
 * Dynamic dual-source application-update description for Thundercall.
 * Supports updates from RozeHub website and GitHub Releases fallback.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AppUpdate {
    public String project = "thundercall";
    public String currentVersion;
    public String latestVersion;
    public boolean available;
    public boolean mandatory;
    public String channel = "Stable";
    public String platform;
    public String architecture;
    public String minimumSupportedVersion;
    public Release release;
    public String source = "website"; // "website" or "github"
    public String htmlUrl;
    public ReleaseArtifact updateArtifact;
    public ReleaseArtifact installerArtifact;
    public String releaseNotes;

    public AppUpdate() {}

    public AppUpdate(
            String project,
            String currentVersion,
            String latestVersion,
            boolean available,
            boolean mandatory,
            String minimumSupportedVersion,
            ReleaseArtifact updateArtifact,
            ReleaseArtifact installerArtifact,
            String releaseNotes
    ) {
        this.project = project;
        this.currentVersion = currentVersion;
        this.latestVersion = latestVersion;
        this.available = available;
        this.mandatory = mandatory;
        this.minimumSupportedVersion = minimumSupportedVersion;
        this.updateArtifact = updateArtifact;
        this.installerArtifact = installerArtifact;
        this.releaseNotes = releaseNotes;

        this.release = new Release();
        this.release.version = latestVersion;
        this.release.channel = channel;
        this.release.notes = releaseNotes;
        this.release.mandatory = mandatory;

        ReleaseArtifact art = preferredUpdateArtifact();
        if (art != null) {
            this.release.fileName = art.fileName();
            this.release.fileSize = art.fileSize();
            this.release.sha256 = art.sha256();
            this.release.downloadUrl = art.downloadUrl();
        }
    }

    // Accessors for backward compatibility
    public String project() { return project; }
    public String currentVersion() { return currentVersion; }
    public String latestVersion() {
        if (latestVersion != null && !latestVersion.isBlank()) return latestVersion;
        if (release != null && release.version != null && !release.version.isBlank()) return release.version;
        return currentVersion;
    }
    public boolean available() { return available; }
    public boolean mandatory() { return mandatory || (release != null && release.mandatory); }
    public String minimumSupportedVersion() { return minimumSupportedVersion; }
    public ReleaseArtifact updateArtifact() { return updateArtifact; }
    public ReleaseArtifact installerArtifact() { return installerArtifact; }
    public String releaseNotes() {
        if (releaseNotes != null && !releaseNotes.isBlank()) return releaseNotes;
        return release != null ? release.displayNotes() : "";
    }

    public ReleaseArtifact preferredUpdateArtifact() {
        if (updateArtifact != null && updateArtifact.isUsable()) return updateArtifact;
        if (installerArtifact != null && installerArtifact.isUsable()) return installerArtifact;
        if (release != null) {
            String fName = release.effectiveFileName();
            String dUrl = release.effectiveDownloadUrl();
            if (fName != null && !fName.isBlank() && dUrl != null && !dUrl.isBlank()) {
                return new ReleaseArtifact(
                        release.id,
                        "UPDATER",
                        release.effectivePackageType(),
                        platform != null ? platform : AppUpdateService.platform(),
                        architecture != null ? architecture : AppUpdateService.architecture(),
                        fName,
                        release.effectiveFileSize(),
                        release.effectiveSha256() != null ? release.effectiveSha256() : "",
                        dUrl
                );
            }
        }
        return null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
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
    ) {
        public boolean isUsable() {
            return fileName != null && !fileName.isBlank()
                    && downloadUrl != null && !downloadUrl.isBlank();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Release {
        public long id;
        public String version;
        public String channel;
        public String notes;
        public String minimumVersion;
        public boolean mandatory;
        public String htmlUrl;

        // Release-level package fields
        public String fileName;
        public long fileSize;
        public String sha256;
        public String downloadUrl;

        public Artifact installerArtifact;
        public Artifact updateArtifact;
        public String publishedAt;

        public Artifact effectiveUpdateArtifact() {
            if (updateArtifact != null && updateArtifact.isUsable()) return updateArtifact;
            if (installerArtifact != null && installerArtifact.isUsable()) return installerArtifact;
            if (fileName != null && !fileName.isBlank()) {
                Artifact legacy = new Artifact();
                legacy.purpose = "UPDATER";
                legacy.packageType = extensionOf(fileName);
                legacy.fileName = fileName;
                legacy.fileSize = fileSize;
                legacy.sha256 = sha256;
                legacy.downloadUrl = downloadUrl;
                return legacy;
            }
            return null;
        }

        public String effectiveFileName() {
            Artifact artifact = effectiveUpdateArtifact();
            return artifact == null ? fileName : artifact.fileName;
        }

        public long effectiveFileSize() {
            Artifact artifact = effectiveUpdateArtifact();
            return artifact == null ? fileSize : artifact.fileSize;
        }

        public String effectiveSha256() {
            Artifact artifact = effectiveUpdateArtifact();
            return artifact == null ? sha256 : artifact.sha256;
        }

        public String effectiveDownloadUrl() {
            Artifact artifact = effectiveUpdateArtifact();
            return artifact == null ? downloadUrl : artifact.downloadUrl;
        }

        public String effectivePackageType() {
            Artifact artifact = effectiveUpdateArtifact();
            if (artifact != null && artifact.packageType != null && !artifact.packageType.isBlank()) {
                return artifact.packageType.toLowerCase(Locale.ROOT);
            }
            return extensionOf(effectiveFileName());
        }

        public String displayNotes() {
            return notes == null || notes.isBlank() ? "No release notes were provided." : notes;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Artifact {
        public long id;
        public String purpose;
        public String packageType;
        public String fileName;
        public long fileSize;
        public String sha256;
        public String downloadUrl;

        public boolean isUsable() {
            return fileName != null && !fileName.isBlank()
                    && downloadUrl != null && !downloadUrl.isBlank();
        }
    }

    private static String extensionOf(String fileName) {
        if (fileName == null) return "";
        String name = fileName.trim();
        int dot = name.lastIndexOf('.');
        return dot >= 0 && dot + 1 < name.length() ? name.substring(dot + 1) : "";
    }
}
