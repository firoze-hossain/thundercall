package com.roze.thundercall.ui.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/**
 * Dynamic dual-source application-update client for Thundercall.
 * Priority 1: Checks website / RozeHub API endpoint.
 * Priority 2: Falls back smoothly to GitHub Releases (firoze-hossain/thundercall)
 * if the website is offline, unlaunched, or has no new release.
 */
public final class AppUpdateService {
    public static final String PRODUCT_SLUG = "thundercall";
    public static final String DEFAULT_ENDPOINT = "http://127.0.0.1:8000/api/v1/updates/thundercall";
    public static final String DEFAULT_GITHUB_REPO = "firoze-hossain/thundercall";
    public static final String CURRENT_VERSION = "1.0.0";

    private static final String PREF_AUTO_CHECK = "updates.autoCheck";
    private static final String PREF_CHANNEL = "updates.channel";
    private static final String PREF_ENDPOINT = "updates.endpoint";
    private static final String PREF_GITHUB_REPO = "updates.githubRepo";

    private static final Path UPDATE_DIR = Path.of(System.getProperty("user.home"), ".thundercall", "updates");
    private static final Preferences PREFS = Preferences.userNodeForPackage(AppUpdateService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private AppUpdateService() {}

    public static boolean isAutoCheckEnabled() { return PREFS.getBoolean(PREF_AUTO_CHECK, true); }
    public static void setAutoCheckEnabled(boolean enabled) { PREFS.putBoolean(PREF_AUTO_CHECK, enabled); }
    public static String getChannel() { return PREFS.get(PREF_CHANNEL, "Stable"); }
    public static void setChannel(String channel) { PREFS.put(PREF_CHANNEL, channel == null ? "Stable" : channel); }
    public static String getEndpoint() { return PREFS.get(PREF_ENDPOINT, DEFAULT_ENDPOINT); }
    public static void setEndpoint(String endpoint) { PREFS.put(PREF_ENDPOINT, endpoint == null || endpoint.isBlank() ? DEFAULT_ENDPOINT : endpoint.trim()); }
    public static String getGitHubRepo() { return PREFS.get(PREF_GITHUB_REPO, DEFAULT_GITHUB_REPO); }
    public static void setGitHubRepo(String repo) { PREFS.put(PREF_GITHUB_REPO, repo == null || repo.isBlank() ? DEFAULT_GITHUB_REPO : repo.trim()); }

    public static String currentVersion() {
        String version = AppUpdateService.class.getPackage().getImplementationVersion();
        if (version == null || version.isBlank()) {
            return CURRENT_VERSION;
        }
        return version.startsWith("v") || version.startsWith("V") ? version.substring(1) : version;
    }

    public static String platform() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) return "Windows";
        if (os.contains("mac") || os.contains("darwin")) return "macOS";
        if (os.contains("linux")) return "Linux";
        return "Linux";
    }

    public static String architecture() {
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (arch.contains("aarch64") || arch.contains("arm64")) return "ARM64";
        if (arch.contains("x86_64") || arch.contains("amd64") || arch.equals("x64")) return "x64";
        return arch;
    }

    /**
     * Compares two semantic version strings (e.g., "1.0.1" vs "1.0.0", "v2026.1.4" vs "1.0.0").
     * Returns > 0 if v1 > v2, < 0 if v1 < v2, and 0 if equal.
     */
    public static int compareVersions(String v1, String v2) {
        if (v1 == null && v2 == null) return 0;
        if (v1 == null) return -1;
        if (v2 == null) return 1;

        String clean1 = v1.trim().replaceFirst("^[vV]", "").split("[-_+]")[0];
        String clean2 = v2.trim().replaceFirst("^[vV]", "").split("[-_+]")[0];

        String[] parts1 = clean1.split("\\.");
        String[] parts2 = clean2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            long p1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            long p2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;
            if (p1 != p2) {
                return Long.compare(p1, p2);
            }
        }
        return 0;
    }

    private static long parseVersionPart(String part) {
        if (part == null || part.isBlank()) return 0;
        try {
            String digits = part.replaceAll("[^0-9].*$", "");
            return digits.isEmpty() ? 0 : Long.parseLong(digits);
        } catch (Exception e) {
            return 0;
        }
    }

    public static boolean isNewerVersion(String candidate, String current) {
        return compareVersions(candidate, current) > 0;
    }

    /**
     * Synchronous check for update with fallback, safe for non-UI virtual threads.
     */
    public static AppUpdate checkForUpdate() {
        try {
            return checkForUpdateAsync().get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            AppUpdate fallback = new AppUpdate();
            fallback.project = PRODUCT_SLUG;
            fallback.currentVersion = currentVersion();
            fallback.latestVersion = currentVersion();
            fallback.available = false;
            return fallback;
        }
    }

    public static CompletableFuture<AppUpdate> checkForUpdateAsync() {
        return checkForUpdateAsync(getEndpoint(), getChannel(), getGitHubRepo());
    }

    public static CompletableFuture<AppUpdate> checkForUpdateAsync(String endpoint, String channel) {
        return checkForUpdateAsync(endpoint, channel, getGitHubRepo());
    }

    /**
     * Priority 1: Checks website / RozeHub API endpoint.
     * Priority 2: Falls back smoothly to GitHub Releases (firoze-hossain/thundercall)
     * if the website is offline, unlaunched, or has no new release.
     */
    public static CompletableFuture<AppUpdate> checkForUpdateAsync(String endpoint, String channel, String githubRepo) {
        return checkForUpdateFromWebsite(endpoint, channel)
                .thenCompose(websiteUpdate -> {
                    if (websiteUpdate != null && websiteUpdate.available
                            && isNewerVersion(websiteUpdate.latestVersion(), currentVersion())) {
                        websiteUpdate.source = "website";
                        return CompletableFuture.completedFuture(websiteUpdate);
                    }
                    return checkForUpdateFromGitHub(githubRepo, channel);
                })
                .exceptionallyCompose(error -> checkForUpdateFromGitHub(githubRepo, channel));
    }

    /**
     * Priority 1: Check website/RozeHub update API.
     */
    public static CompletableFuture<AppUpdate> checkForUpdateFromWebsite(String endpoint, String channel) {
        String base = (endpoint == null || endpoint.isBlank()) ? DEFAULT_ENDPOINT : endpoint.trim();
        String separator = base.contains("?") ? "&" : "?";
        String uri = base + separator
                + "version=" + encode(currentVersion())
                + "&platform=" + encode(platform())
                + "&architecture=" + encode(architecture())
                + "&arch=" + encode(architecture())
                + "&channel=" + encode(channel == null || channel.isBlank() ? "Stable" : channel);

        HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                .timeout(Duration.ofSeconds(4)) // 4s timeout for quick fallback
                .header("Accept", "application/json")
                .header("User-Agent", "ThunderCall/" + currentVersion())
                .GET()
                .build();

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenCompose(response -> {
                    if (response.statusCode() / 100 != 2) {
                        return CompletableFuture.failedFuture(
                                new IOException("Website update server returned HTTP " + response.statusCode()));
                    }
                    try {
                        JsonNode root = MAPPER.readTree(response.body());
                        AppUpdate update = parseWebsiteUpdate(root, base, channel);
                        return CompletableFuture.completedFuture(update);
                    } catch (Exception e) {
                        return CompletableFuture.failedFuture(new IOException("Invalid website update response", e));
                    }
                });
    }

    private static AppUpdate parseWebsiteUpdate(JsonNode root, String endpointBase, String channel) {
        AppUpdate update = new AppUpdate();
        update.project = root.path("project").asText(PRODUCT_SLUG);
        update.currentVersion = currentVersion();

        boolean available = root.path("available").asBoolean(root.path("updateAvailable").asBoolean(false));
        update.available = available;
        update.mandatory = root.path("mandatory").asBoolean(false);
        update.channel = channel != null ? channel : "Stable";
        update.platform = platform();
        update.architecture = architecture();
        update.source = "website";

        String latestVer = root.path("latestVersion").asText("");
        JsonNode releaseNode = root.path("release");

        if (latestVer.isBlank() && !releaseNode.isMissingNode()) {
            latestVer = releaseNode.path("version").asText("");
        }
        if (latestVer.isBlank()) {
            latestVer = currentVersion();
        }
        update.latestVersion = latestVer;

        if (isNewerVersion(latestVer, currentVersion())) {
            update.available = true;
        }

        // Parse release info if present
        if (!releaseNode.isMissingNode() && releaseNode.isObject()) {
            AppUpdate.Release release = new AppUpdate.Release();
            release.version = latestVer;
            release.channel = channel;
            release.notes = releaseNode.path("notes").asText(releaseNode.path("releaseNotes").asText(""));
            release.fileName = releaseNode.path("fileName").asText("");
            release.fileSize = releaseNode.path("fileSize").asLong(0);
            release.sha256 = releaseNode.path("sha256").asText("");
            String dUrl = releaseNode.path("downloadUrl").asText("");
            if (dUrl.startsWith("/") && endpointBase != null) {
                try {
                    URI baseUri = URI.create(endpointBase);
                    dUrl = baseUri.getScheme() + "://" + baseUri.getAuthority() + dUrl;
                } catch (Exception ignored) {}
            }
            release.downloadUrl = dUrl;
            release.mandatory = releaseNode.path("mandatory").asBoolean(update.mandatory);
            update.release = release;
        }

        // Parse updateArtifact and installerArtifact
        JsonNode updateArtNode = root.path("updateArtifact");
        if (updateArtNode.isMissingNode() && !releaseNode.isMissingNode()) {
            updateArtNode = releaseNode.path("updateArtifact");
        }
        if (!updateArtNode.isMissingNode() && updateArtNode.isObject()) {
            update.updateArtifact = parseArtifactNode(updateArtNode, endpointBase);
        }

        JsonNode installerArtNode = root.path("installerArtifact");
        if (installerArtNode.isMissingNode() && !releaseNode.isMissingNode()) {
            installerArtNode = releaseNode.path("installerArtifact");
        }
        if (!installerArtNode.isMissingNode() && installerArtNode.isObject()) {
            update.installerArtifact = parseArtifactNode(installerArtNode, endpointBase);
        }

        if (update.release == null) {
            AppUpdate.ReleaseArtifact best = update.preferredUpdateArtifact();
            if (best != null) {
                AppUpdate.Release rel = new AppUpdate.Release();
                rel.version = latestVer;
                rel.channel = channel;
                rel.notes = root.path("releaseNotes").asText("");
                rel.fileName = best.fileName();
                rel.fileSize = best.fileSize();
                rel.sha256 = best.sha256();
                rel.downloadUrl = best.downloadUrl();
                rel.mandatory = update.mandatory;
                update.release = rel;
            }
        }

        return update;
    }

    private static AppUpdate.ReleaseArtifact parseArtifactNode(JsonNode node, String endpointBase) {
        String dUrl = node.path("downloadUrl").asText("");
        if (dUrl.startsWith("/") && endpointBase != null) {
            try {
                URI baseUri = URI.create(endpointBase);
                dUrl = baseUri.getScheme() + "://" + baseUri.getAuthority() + dUrl;
            } catch (Exception ignored) {}
        }
        return new AppUpdate.ReleaseArtifact(
                node.path("id").asLong(0),
                node.path("purpose").asText(""),
                node.path("packageType").asText(""),
                node.path("platform").asText(platform()),
                node.path("architecture").asText(architecture()),
                node.path("fileName").asText(""),
                node.path("fileSize").asLong(0),
                node.path("sha256").asText(""),
                dUrl
        );
    }

    /**
     * Priority 2: Check GitHub Releases API for the repository.
     */
    public static CompletableFuture<AppUpdate> checkForUpdateFromGitHub(String repo, String channel) {
        String targetRepo = (repo == null || repo.isBlank()) ? DEFAULT_GITHUB_REPO : repo.trim();
        String latestUrl = "https://api.github.com/repos/" + targetRepo + "/releases/latest";

        HttpRequest request = HttpRequest.newBuilder(URI.create(latestUrl))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "ThunderCall/" + currentVersion())
                .GET()
                .build();

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenCompose(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonNode rootNode = MAPPER.readTree(response.body());
                            return CompletableFuture.completedFuture(parseGitHubRelease(rootNode, currentVersion(), channel, targetRepo));
                        } catch (Exception e) {
                            return CompletableFuture.failedFuture(e);
                        }
                    } else if (response.statusCode() == 404) {
                        return queryGitHubReleasesList(targetRepo, channel);
                    } else {
                        return CompletableFuture.failedFuture(
                                new IOException("GitHub API returned HTTP " + response.statusCode()));
                    }
                })
                .exceptionallyCompose(err -> queryGitHubTagsFallback(targetRepo, channel));
    }

    private static CompletableFuture<AppUpdate> queryGitHubReleasesList(String targetRepo, String channel) {
        String releasesUrl = "https://api.github.com/repos/" + targetRepo + "/releases";
        HttpRequest request = HttpRequest.newBuilder(URI.create(releasesUrl))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "ThunderCall/" + currentVersion())
                .GET()
                .build();

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenCompose(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonNode array = MAPPER.readTree(response.body());
                            if (array.isArray() && array.size() > 0) {
                                for (JsonNode item : array) {
                                    if (!item.path("draft").asBoolean(false)) {
                                        return CompletableFuture.completedFuture(parseGitHubRelease(item, currentVersion(), channel, targetRepo));
                                    }
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                    return queryGitHubTagsFallback(targetRepo, channel);
                });
    }

    private static CompletableFuture<AppUpdate> queryGitHubTagsFallback(String targetRepo, String channel) {
        String tagsUrl = "https://api.github.com/repos/" + targetRepo + "/tags";
        HttpRequest request = HttpRequest.newBuilder(URI.create(tagsUrl))
                .timeout(Duration.ofSeconds(6))
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "ThunderCall/" + currentVersion())
                .GET()
                .build();

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonNode array = MAPPER.readTree(response.body());
                            if (array.isArray() && array.size() > 0) {
                                JsonNode first = array.get(0);
                                String tagName = first.path("name").asText("");
                                String cleanVer = tagName.replaceFirst("^[vV]", "");
                                boolean newer = isNewerVersion(cleanVer, currentVersion());

                                AppUpdate update = new AppUpdate();
                                update.source = "github";
                                update.project = PRODUCT_SLUG;
                                update.currentVersion = currentVersion();
                                update.latestVersion = cleanVer;
                                update.available = newer;
                                update.channel = channel != null ? channel : "Stable";
                                update.platform = platform();
                                update.architecture = architecture();

                                if (newer) {
                                    AppUpdate.Release rel = new AppUpdate.Release();
                                    rel.version = cleanVer;
                                    rel.channel = channel;
                                    rel.notes = "New release tag " + tagName + " published on GitHub (" + targetRepo + ").";
                                    rel.htmlUrl = "https://github.com/" + targetRepo + "/releases/tag/" + tagName;
                                    rel.fileName = "thundercall-" + cleanVer + ".deb";
                                    rel.downloadUrl = rel.htmlUrl;
                                    update.release = rel;
                                }
                                return update;
                            }
                        } catch (Exception ignored) {}
                    }
                    AppUpdate noUpdate = new AppUpdate();
                    noUpdate.source = "github";
                    noUpdate.project = PRODUCT_SLUG;
                    noUpdate.currentVersion = currentVersion();
                    noUpdate.latestVersion = currentVersion();
                    noUpdate.available = false;
                    return noUpdate;
                });
    }

    public static AppUpdate parseGitHubRelease(JsonNode releaseNode, String currentVersion, String channel, String targetRepo) {
        String tagName = releaseNode.path("tag_name").asText("");
        if (tagName.isBlank()) {
            tagName = releaseNode.path("name").asText("");
        }
        String latestVersion = tagName.replaceFirst("^[vV]", "").trim();
        boolean newer = isNewerVersion(latestVersion, currentVersion);

        AppUpdate update = new AppUpdate();
        update.project = PRODUCT_SLUG;
        update.currentVersion = currentVersion;
        update.latestVersion = latestVersion;
        update.available = newer;
        update.source = "github";
        update.channel = channel != null ? channel : "Stable";
        update.platform = platform();
        update.architecture = architecture();

        AppUpdate.Release release = new AppUpdate.Release();
        release.id = releaseNode.path("id").asLong(0);
        release.version = latestVersion;
        release.channel = channel;
        release.notes = releaseNode.path("body").asText("No release notes were provided.");
        release.publishedAt = releaseNode.path("published_at").asText("");
        release.htmlUrl = releaseNode.path("html_url").asText("");

        // Select best matching asset for OS & Architecture
        String os = platform();
        String arch = architecture();
        JsonNode assets = releaseNode.path("assets");
        JsonNode bestAsset = null;
        int bestScore = -1;

        if (assets.isArray()) {
            for (JsonNode asset : assets) {
                String name = asset.path("name").asText("").toLowerCase(Locale.ROOT);
                int score = scoreAsset(name, os, arch);
                if (score > bestScore) {
                    bestScore = score;
                    bestAsset = asset;
                }
            }
        }

        if (bestAsset != null) {
            release.fileName = bestAsset.path("name").asText("");
            release.fileSize = bestAsset.path("size").asLong(0);
            release.downloadUrl = bestAsset.path("browser_download_url").asText("");
        } else {
            release.fileName = "thundercall-" + latestVersion + (os.equals("Windows") ? ".exe" : (os.equals("macOS") ? ".dmg" : ".deb"));
            release.downloadUrl = releaseNode.path("zipball_url").asText(release.htmlUrl);
        }

        update.release = release;
        return update;
    }

    private static int scoreAsset(String name, String os, String arch) {
        int score = 0;
        if ("Linux".equals(os)) {
            if (name.endsWith(".deb")) score += 1000;
            else if (name.endsWith(".appimage")) score += 800;
            else if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) score += 600;
            else if (name.endsWith(".rpm")) score += 300;
            else if (name.endsWith(".zip")) score += 200;

            if (score > 0) {
                if ("ARM64".equals(arch) && (name.contains("arm64") || name.contains("aarch64"))) score += 300;
                if ("x64".equals(arch) && (name.contains("x86_64") || name.contains("amd64") || name.contains("x64"))) score += 300;
            }
        } else if ("Windows".equals(os)) {
            if (name.endsWith(".exe")) score += 1000;
            else if (name.endsWith(".msi")) score += 900;
            else if (name.endsWith(".zip")) score += 400;

            if (score > 0) {
                if ("ARM64".equals(arch) && name.contains("arm64")) score += 300;
                if ("x64".equals(arch) && (name.contains("x64") || name.contains("x86_64") || name.contains("win64"))) score += 300;
            }
        } else if ("macOS".equals(os)) {
            if (name.endsWith(".dmg")) score += 1000;
            else if (name.endsWith(".pkg")) score += 900;
            else if (name.endsWith(".zip")) score += 400;

            if (score > 0) {
                if ("ARM64".equals(arch) && (name.contains("arm64") || name.contains("aarch64"))) score += 300;
                if ("x64".equals(arch) && (name.contains("x86_64") || name.contains("x64"))) score += 300;
            }
        }
        return score;
    }

    public static CompletableFuture<Path> download(AppUpdate.Release release, Consumer<Double> progress) {
        if (release == null) {
            return CompletableFuture.failedFuture(new IOException("No update release was provided."));
        }
        String downloadUrl = release.effectiveDownloadUrl();
        String fileName = release.effectiveFileName();
        long fileSize = release.effectiveFileSize();
        String expectedSha256 = release.effectiveSha256();

        if (downloadUrl == null || downloadUrl.isBlank()) {
            return CompletableFuture.failedFuture(new IOException("No download URL was provided for update."));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                Files.createDirectories(UPDATE_DIR);
                String safeName = sanitizeFileName(fileName);
                Path partial = UPDATE_DIR.resolve(safeName + ".part");
                Path target = UPDATE_DIR.resolve(safeName);
                Files.deleteIfExists(partial);

                HttpRequest request = HttpRequest.newBuilder(URI.create(downloadUrl))
                        .timeout(Duration.ofMinutes(30))
                        .header("Accept", "application/octet-stream")
                        .header("User-Agent", "ThunderCall/" + currentVersion())
                        .GET()
                        .build();

                HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() / 100 != 2) {
                    throw new IOException("Download failed with HTTP " + response.statusCode());
                }

                long expectedSize = fileSize;
                if (expectedSize <= 0) {
                    expectedSize = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
                }

                long downloaded = 0;
                try (InputStream in = response.body();
                     OutputStream out = Files.newOutputStream(partial, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    byte[] buffer = new byte[1024 * 1024];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                        downloaded += read;
                        if (progress != null && expectedSize > 0) {
                            progress.accept(Math.min(1d, downloaded / (double) expectedSize));
                        }
                    }
                }

                if (expectedSize > 0 && downloaded != expectedSize) {
                    throw new IOException("Downloaded file size (" + downloaded + ") does not match metadata (" + expectedSize + ").");
                }

                if (expectedSha256 != null && !expectedSha256.isBlank()) {
                    String actualHash = sha256(partial);
                    if (!actualHash.equalsIgnoreCase(expectedSha256.trim())) {
                        Files.deleteIfExists(partial);
                        throw new IOException("SHA-256 verification failed. The downloaded package was not verified.");
                    }
                }

                Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                if (progress != null) progress.accept(1d);
                return target;
            } catch (Exception e) {
                try { Files.deleteIfExists(UPDATE_DIR.resolve(sanitizeFileName(fileName) + ".part")); }
                catch (Exception ignored) {}
                throw new RuntimeException(e.getMessage(), e);
            }
        });
    }

    /** Backward compatible downloadAndVerify for ReleaseArtifact. */
    public static Path downloadAndVerify(AppUpdate.ReleaseArtifact artifact, DownloadProgress progress)
            throws IOException, InterruptedException {
        if (artifact == null || artifact.downloadUrl() == null || artifact.downloadUrl().isBlank()) {
            throw new IOException("Update download URL was not provided.");
        }
        Files.createDirectories(UPDATE_DIR);
        String safeName = sanitizeFileName(artifact.fileName());
        Path part = UPDATE_DIR.resolve(safeName + ".part");
        Path target = UPDATE_DIR.resolve(safeName);

        HttpRequest request = HttpRequest.newBuilder(URI.create(artifact.downloadUrl()))
                .timeout(Duration.ofMinutes(30))
                .header("Accept", "application/octet-stream")
                .header("User-Agent", "ThunderCall/" + currentVersion())
                .GET()
                .build();

        HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Download failed: HTTP " + response.statusCode());
        }

        long expected = artifact.fileSize();
        long total = 0;
        try (InputStream in = response.body();
             OutputStream out = Files.newOutputStream(part, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            byte[] buffer = new byte[1024 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                total += read;
                if (progress != null) progress.onProgress(total, expected);
            }
        } catch (Exception e) {
            Files.deleteIfExists(part);
            throw e;
        }

        if (expected > 0 && total != expected) {
            Files.deleteIfExists(part);
            throw new IOException("Downloaded size does not match package metadata.");
        }

        if (artifact.sha256() != null && !artifact.sha256().isBlank()) {
            try {
                String actual = sha256(part);
                if (!actual.equalsIgnoreCase(artifact.sha256())) {
                    Files.deleteIfExists(part);
                    throw new IOException("SHA-256 verification failed.");
                }
            } catch (Exception e) {
                Files.deleteIfExists(part);
                throw new IOException("SHA-256 calculation failed", e);
            }
        }

        Files.move(part, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return target;
    }

    /** Starts a detached helper or installer, then exits the JavaFX process cleanly. */
    public static void installAndRestart(Path packageFile) throws IOException {
        installAndRestart(packageFile, null);
    }

    public static void installAndRestart(Path packageFile, AppUpdate.ReleaseArtifact artifact) throws IOException {
        if (packageFile == null || !Files.isRegularFile(packageFile)) {
            throw new IOException("Update package was not found: " + packageFile);
        }

        String pkg = (artifact != null && artifact.packageType() != null && !artifact.packageType().isBlank())
                ? artifact.packageType().toLowerCase(Locale.ROOT)
                : extensionOf(packageFile.getFileName().toString());

        try {
            Path script = platformScript(UPDATE_DIR, packageFile, pkg);
            ProcessBuilder pb;
            if (platform().equals("Windows")) {
                pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", script.toString());
            } else {
                pb = new ProcessBuilder("sh", script.toString());
            }
            pb.directory(UPDATE_DIR.toFile());
            pb.start();
        } catch (Exception scriptEx) {
            // Direct execution fallback if script fails
            directExecute(packageFile);
        }

        javafx.application.Platform.exit();
        System.exit(0);
    }

    private static void directExecute(Path packageFile) throws IOException {
        String name = packageFile.getFileName().toString().toLowerCase(Locale.ROOT);
        String platform = platform();
        ProcessBuilder builder;

        if (platform.equals("Windows")) {
            if (name.endsWith(".msi")) {
                builder = new ProcessBuilder("msiexec.exe", "/i", packageFile.toAbsolutePath().toString());
            } else if (name.endsWith(".exe")) {
                builder = new ProcessBuilder(packageFile.toAbsolutePath().toString());
            } else {
                builder = new ProcessBuilder("explorer.exe", "/select,", packageFile.toAbsolutePath().toString());
            }
        } else if (platform.equals("macOS")) {
            if (name.endsWith(".pkg")) {
                builder = new ProcessBuilder("open", "-W", packageFile.toAbsolutePath().toString());
            } else if (name.endsWith(".dmg")) {
                builder = new ProcessBuilder("open", packageFile.toAbsolutePath().toString());
            } else {
                builder = new ProcessBuilder("open", "-R", packageFile.toAbsolutePath().toString());
            }
        } else {
            if (name.endsWith(".deb")) {
                try {
                    builder = new ProcessBuilder("pkexec", "dpkg", "-i", packageFile.toAbsolutePath().toString());
                } catch (Exception e) {
                    builder = new ProcessBuilder("xdg-open", packageFile.toAbsolutePath().toString());
                }
            } else if (name.endsWith(".appimage")) {
                packageFile.toFile().setExecutable(true, false);
                builder = new ProcessBuilder(packageFile.toAbsolutePath().toString());
            } else {
                builder = new ProcessBuilder("xdg-open", packageFile.getParent().toAbsolutePath().toString());
            }
        }
        builder.start();
    }

    private static Path platformScript(Path dir, Path packageFile, String pkg) throws IOException {
        String appCommand = ProcessHandle.current().info().command().orElse("thundercall");
        long pid = ProcessHandle.current().pid();
        Path script = dir.resolve(platform().equals("Windows") ? "thundercall-update.ps1" : "thundercall-update.sh");

        if (platform().equals("Windows")) {
            String install = switch (pkg) {
                case "msi" -> "Start-Process msiexec.exe -ArgumentList @('/i', $package, '/qn') -Wait";
                case "exe" -> "Start-Process $package -Wait";
                default -> "Start-Process $package -Wait";
            };
            String app = appCommand.replace("\\", "\\\\").replace("'", "''");
            String text = "$package = '" + ps(packageFile.toAbsolutePath()) + "'\n"
                    + "while (Get-Process -Id " + pid + " -ErrorAction SilentlyContinue) { Start-Sleep -Milliseconds 300 }\n"
                    + install + "\nStart-Process '" + app + "'\n";
            Files.writeString(script, text, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } else {
            String install = switch (pkg) {
                case "deb" -> "pkexec dpkg -i \"$PACKAGE\" || pkexec apt-get install -y \"$PACKAGE\"";
                case "pkg" -> "sudo installer -pkg \"$PACKAGE\" -target /";
                case "dmg" -> "open \"$PACKAGE\"";
                case "appimage" -> "chmod +x \"$PACKAGE\" && \"$PACKAGE\" &";
                default -> "xdg-open \"$PACKAGE\"";
            };
            String text = "#!/bin/sh\nPACKAGE=\"" + sh(packageFile.toAbsolutePath()) + "\"\n"
                    + "while kill -0 " + pid + " 2>/dev/null; do sleep 0.3; done\n"
                    + install + "\n"
                    + "sleep 1\n\"" + sh(Path.of(appCommand)) + "\" >/dev/null 2>&1 &\n";
            Files.writeString(script, text, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            script.toFile().setExecutable(true);
        }
        return script;
    }

    private static String extensionOf(String fileName) {
        if (fileName == null) return "";
        String name = fileName.trim();
        int dot = name.lastIndexOf('.');
        return dot >= 0 && dot + 1 < name.length() ? name.substring(dot + 1) : "";
    }

    private static String sanitizeFileName(String n) {
        String name = (n == null || n.isBlank()) ? "thundercall-update" : Path.of(n).getFileName().toString();
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String ps(Path p) {
        return p.toString().replace("'", "''");
    }

    private static String sh(Path p) {
        return p.toString().replace("\"", "\\\"");
    }

    private static String sha256(Path file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(file)) {
            byte[] b = new byte[1024 * 1024];
            int n;
            while ((n = in.read(b)) != -1) md.update(b, 0, n);
        }
        StringBuilder s = new StringBuilder(64);
        for (byte x : md.digest()) s.append(String.format("%02x", x));
        return s.toString();
    }

    public interface DownloadProgress {
        void onProgress(long downloaded, long total);
    }
}
