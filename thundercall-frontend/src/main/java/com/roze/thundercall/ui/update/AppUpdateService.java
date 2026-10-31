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
        if (version != null && !version.isBlank() && !"1.0.0".equals(version.trim())) {
            return version.startsWith("v") || version.startsWith("V") ? version.substring(1) : version;
        }

        // Check for app bundle version in Info.plist (macOS) or ThunderCall.cfg (Windows/Linux)
        String appVersion = detectAppBundleVersion();
        if (appVersion != null && !appVersion.isBlank()) {
            return appVersion.startsWith("v") || appVersion.startsWith("V") ? appVersion.substring(1) : appVersion;
        }

        if (version != null && !version.isBlank()) {
            return version.startsWith("v") || version.startsWith("V") ? version.substring(1) : version;
        }
        return CURRENT_VERSION;
    }

    private static String detectAppBundleVersion() {
        try {
            Path installDir = getInstallationDirectory();
            if (installDir == null) return null;

            if ("macOS".equals(platform())) {
                Path plist = installDir.resolve("Contents/Info.plist");
                if (Files.exists(plist)) {
                    String content = Files.readString(plist);
                    java.util.regex.Matcher m = java.util.regex.Pattern
                            .compile("<key>CFBundleShortVersionString</key>\\s*<string>([^<]+)</string>")
                            .matcher(content);
                    if (m.find()) return m.group(1).trim();
                }
            } else {
                Path cfg = installDir.resolve("app/ThunderCall.cfg");
                if (!Files.exists(cfg)) cfg = installDir.resolve("ThunderCall.cfg");
                if (!Files.exists(cfg)) cfg = installDir.resolve("lib/app/thundercall.cfg");
                if (Files.exists(cfg)) {
                    for (String line : Files.readAllLines(cfg)) {
                        if (line.trim().startsWith("app.version=")) {
                            return line.substring(line.indexOf('=') + 1).trim();
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
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

    public static Path getInstallationDirectory() {
        try {
            var cmdOpt = ProcessHandle.current().info().command();
            if (cmdOpt.isPresent() && !cmdOpt.get().isBlank()) {
                Path execPath = Path.of(cmdOpt.get()).toAbsolutePath();
                if (Files.exists(execPath)) {
                    if ("macOS".equals(platform())) {
                        Path curr = execPath;
                        while (curr != null) {
                            if (curr.getFileName() != null && curr.getFileName().toString().endsWith(".app")) {
                                return curr;
                            }
                            curr = curr.getParent();
                        }
                    } else {
                        return execPath.getParent();
                    }
                }
            }
        } catch (Throwable ignored) {}

        try {
            var loc = AppUpdateService.class.getProtectionDomain().getCodeSource().getLocation();
            if (loc != null) {
                Path jarPath = Path.of(loc.toURI()).toAbsolutePath();
                if ("macOS".equals(platform())) {
                    Path curr = jarPath;
                    while (curr != null) {
                        if (curr.getFileName() != null && curr.getFileName().toString().endsWith(".app")) {
                            return curr;
                        }
                        curr = curr.getParent();
                    }
                } else {
                    if (jarPath.getParent() != null && "app".equalsIgnoreCase(jarPath.getParent().getFileName().toString())) {
                        return jarPath.getParent().getParent();
                    }
                    return jarPath.getParent();
                }
            }
        } catch (Throwable ignored) {}

        if ("macOS".equals(platform())) {
            Path defaultApp = Path.of("/Applications/ThunderCall.app");
            if (Files.isDirectory(defaultApp)) return defaultApp;
            Path userApp = Path.of(System.getProperty("user.home"), "Applications", "ThunderCall.app");
            if (Files.isDirectory(userApp)) return userApp;
            return defaultApp;
        } else if ("Windows".equals(platform())) {
            Path localProg = Path.of(System.getProperty("user.home"), "AppData", "Local", "Programs", "ThunderCall");
            if (Files.isDirectory(localProg)) return localProg;
            return Path.of("C:\\Program Files\\ThunderCall");
        } else {
            return Path.of("/opt/thundercall");
        }
    }

    private static int scoreAsset(String name, String os, String arch) {
        int score = 0;
        if ("Linux".equals(os)) {
            // Priority 1: In-place update archive (zero-friction update)
            if ((name.contains("linux-update") || name.contains("-update.")) && (name.endsWith(".tar.gz") || name.endsWith(".tgz") || name.endsWith(".zip"))) {
                score += 2000;
            } else if (name.endsWith(".deb")) {
                score += 1000;
            } else if (name.endsWith(".appimage")) {
                score += 800;
            } else if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
                score += 600;
            } else if (name.endsWith(".rpm")) {
                score += 300;
            } else if (name.endsWith(".zip")) {
                score += 200;
            }

            if (score > 0) {
                if ("ARM64".equals(arch) && (name.contains("arm64") || name.contains("aarch64"))) score += 300;
                if ("x64".equals(arch) && (name.contains("x86_64") || name.contains("amd64") || name.contains("x64"))) score += 300;
            }
        } else if ("Windows".equals(os)) {
            // Priority 1: In-place update ZIP package (fastest, no SmartScreen prompt, no installer wizard)
            if ((name.contains("win-update") || name.contains("windows-update") || name.contains("-update.")) && name.endsWith(".zip")) {
                score += 2000;
            }
            // Priority 2: Inno Setup executable setup wizard
            else if (name.endsWith(".exe") && (name.contains("setup") || name.contains("thundercall"))) {
                score += 1200;
            } else if (name.endsWith(".exe")) {
                score += 1000;
            }
            // Priority 3: Generic ZIP
            else if (name.endsWith(".zip")) {
                score += 500;
            }
            // Priority 4: MSI (deprioritized to avoid breaking Inno Setup installations)
            else if (name.endsWith(".msi")) {
                score += 100;
            }

            if (score > 0) {
                if ("ARM64".equals(arch) && name.contains("arm64")) score += 300;
                if ("x64".equals(arch) && (name.contains("x64") || name.contains("x86_64") || name.contains("win64"))) score += 300;
            }
        } else if ("macOS".equals(os)) {
            // Priority 1: In-place macOS update archive (.tar.gz or .zip - zero Gatekeeper quarantine, in-place replacement)
            if ((name.contains("mac-update") || name.contains("macos-update") || name.contains("-update.")) && (name.endsWith(".tar.gz") || name.endsWith(".tgz") || name.endsWith(".zip"))) {
                score += 2000;
            }
            // Priority 2: Apple installer .pkg (installs directly into /Applications)
            else if (name.endsWith(".pkg")) {
                score += 1000;
            }
            // Priority 3: Apple .dmg disk image
            else if (name.endsWith(".dmg")) {
                score += 800;
            }
            // Priority 4: Generic ZIP/tar.gz
            else if (name.endsWith(".tar.gz") || name.endsWith(".tgz") || name.endsWith(".zip")) {
                score += 400;
            }

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

        String name = packageFile.getFileName().toString().toLowerCase(Locale.ROOT);
        String platform = platform();
        long currentPid = ProcessHandle.current().pid();
        Path installDir = getInstallationDirectory();
        Path logFile = UPDATE_DIR.resolve("update.log");

        ProcessBuilder builder;

        if (platform.equals("Windows")) {
            if (name.endsWith(".zip")) {
                // In-place ZIP update: extract directly over install directory without installer prompts or SmartScreen
                Path psScript = UPDATE_DIR.resolve("apply-update.ps1");
                String scriptContent = """
                        param(
                            [int]$ProcessId,
                            [string]$PackagePath,
                            [string]$TargetDir,
                            [string]$LogPath
                        )
                        Start-Transcript -Path $LogPath -Force
                        Write-Host "=== ThunderCall Windows In-Place Updater ==="
                        Write-Host "Process ID: $ProcessId"
                        Write-Host "Package: $PackagePath"
                        Write-Host "Target Directory: $TargetDir"
                        
                        if ($ProcessId -gt 0) {
                            Write-Host "Waiting for ThunderCall to close..."
                            try { Wait-Process -Id $ProcessId -Timeout 30 -ErrorAction SilentlyContinue } catch {}
                        }
                        Start-Sleep -Seconds 1
                        
                        $tempExtract = Join-Path $env:TEMP ("thundercall_win_upd_" + [Guid]::NewGuid().ToString("N"))
                        New-Item -ItemType Directory -Path $tempExtract -Force | Out-Null
                        
                        try {
                            Write-Host "Extracting update package..."
                            Expand-Archive -Path $PackagePath -DestinationPath $tempExtract -Force
                            
                            $subApp = Join-Path $tempExtract "ThunderCall"
                            $sourceDir = if (Test-Path $subApp) { $subApp } else { $tempExtract }
                            
                            Write-Host "Updating application files in $TargetDir..."
                            Copy-Item -Path "$sourceDir\\*" -Destination $TargetDir -Recurse -Force
                        } catch {
                            Write-Host "Error updating files: $_"
                        } finally {
                            Remove-Item -Path $tempExtract -Recurse -Force -ErrorAction SilentlyContinue
                        }
                        
                        $exePath = Join-Path $TargetDir "ThunderCall.exe"
                        if (Test-Path $exePath) {
                            Write-Host "Launching updated $exePath..."
                            Start-Process -FilePath $exePath
                        }
                        Remove-Item -Path $PackagePath -Force -ErrorAction SilentlyContinue
                        Stop-Transcript
                        """;
                Files.writeString(psScript, scriptContent, java.nio.charset.StandardCharsets.UTF_8);

                builder = new ProcessBuilder(
                        "powershell.exe",
                        "-NoProfile",
                        "-WindowStyle", "Hidden",
                        "-ExecutionPolicy", "Bypass",
                        "-File", psScript.toAbsolutePath().toString(),
                        "-ProcessId", String.valueOf(currentPid),
                        "-PackagePath", packageFile.toAbsolutePath().toString(),
                        "-TargetDir", installDir.toAbsolutePath().toString(),
                        "-LogPath", logFile.toAbsolutePath().toString()
                );
            } else if (name.endsWith(".exe")) {
                // Inno Setup installer: run with /VERYSILENT so it updates without user wizard prompts
                builder = new ProcessBuilder(
                        packageFile.toAbsolutePath().toString(),
                        "/VERYSILENT",
                        "/SUPPRESSMSGBOXES",
                        "/NORESTART",
                        "/CLOSEAPPLICATIONS",
                        "/RESTARTAPPLICATIONS",
                        "/DIR=" + installDir.toAbsolutePath().toString()
                );
            } else if (name.endsWith(".msi")) {
                builder = new ProcessBuilder("msiexec.exe", "/i", packageFile.toAbsolutePath().toString(), "/passive", "/norestart");
            } else {
                builder = new ProcessBuilder("explorer.exe", "/select,", packageFile.toAbsolutePath().toString());
            }
        } else if (platform.equals("macOS")) {
            if (name.endsWith(".tar.gz") || name.endsWith(".tgz") || name.endsWith(".zip") || name.endsWith(".dmg")) {
                // In-place macOS update or automated DMG extraction:
                // Replaces /Applications/ThunderCall.app and clears quarantine to prevent Gatekeeper warnings
                Path shScript = UPDATE_DIR.resolve("apply-update.sh");
                String scriptContent = """
                        #!/bin/bash
                        PID=$1
                        PKG="$2"
                        TARGET_APP="$3"
                        LOG="$4"
                        
                        exec > "$LOG" 2>&1
                        echo "=== ThunderCall macOS Updater ==="
                        echo "PID: $PID"
                        echo "PKG: $PKG"
                        echo "TARGET_APP: $TARGET_APP"
                        
                        if [ -n "$PID" ] && [ "$PID" -gt 0 ]; then
                            echo "Waiting for process $PID to exit..."
                            for i in {1..60}; do
                                if ! kill -0 "$PID" 2>/dev/null; then
                                    break
                                fi
                                sleep 0.5
                            done
                        fi
                        
                        if [ -z "$TARGET_APP" ] || [ ! -d "$TARGET_APP" ]; then
                            if [ -d "/Applications/ThunderCall.app" ]; then
                                TARGET_APP="/Applications/ThunderCall.app"
                            elif [ -d "$HOME/Applications/ThunderCall.app" ]; then
                                TARGET_APP="$HOME/Applications/ThunderCall.app"
                            else
                                TARGET_APP="/Applications/ThunderCall.app"
                            fi
                        fi
                        
                        TEMP_DIR=$(mktemp -d /tmp/thundercall_mac_upd.XXXXXX)
                        
                        if [[ "$PKG" == *.tar.gz || "$PKG" == *.tgz ]]; then
                            echo "Unpacking tar.gz update package..."
                            tar -xzf "$PKG" -C "$TEMP_DIR"
                        elif [[ "$PKG" == *.zip ]]; then
                            echo "Unpacking zip update package..."
                            unzip -q -o "$PKG" -d "$TEMP_DIR"
                        elif [[ "$PKG" == *.dmg ]]; then
                            echo "Mounting DMG disk image..."
                            MOUNT_POINT=$(hdiutil attach -nobrowse -readonly "$PKG" 2>&1 | grep -E '/Volumes/' | awk '{print $NF}')
                            echo "Mounted at: $MOUNT_POINT"
                            if [ -d "$MOUNT_POINT/ThunderCall.app" ]; then
                                cp -R "$MOUNT_POINT/ThunderCall.app" "$TEMP_DIR/"
                            fi
                            hdiutil detach "$MOUNT_POINT" -force 2>/dev/null || true
                        fi
                        
                        NEW_APP=$(find "$TEMP_DIR" -maxdepth 3 -name "ThunderCall.app" -type d | head -n 1)
                        
                        if [ -n "$NEW_APP" ] && [ -d "$NEW_APP" ]; then
                            echo "Found updated app at: $NEW_APP"
                            # Strip quarantine attribute to avoid Gatekeeper prompt
                            xattr -dr com.apple.quarantine "$NEW_APP" 2>/dev/null || true
                            chmod -R +x "$NEW_APP/Contents/MacOS" 2>/dev/null || true
                            
                            echo "Replacing $TARGET_APP..."
                            rm -rf "$TARGET_APP"
                            cp -R "$NEW_APP" "$TARGET_APP"
                            xattr -dr com.apple.quarantine "$TARGET_APP" 2>/dev/null || true
                            chmod -R +x "$TARGET_APP/Contents/MacOS" 2>/dev/null || true
                            
                            echo "Relaunching $TARGET_APP..."
                            open -a "$TARGET_APP"
                        else
                            echo "Error: ThunderCall.app not found in package"
                        fi
                        
                        rm -rf "$TEMP_DIR"
                        rm -f "$PKG"
                        """;
                Files.writeString(shScript, scriptContent, java.nio.charset.StandardCharsets.UTF_8);
                shScript.toFile().setExecutable(true, false);

                builder = new ProcessBuilder(
                        "/bin/bash",
                        shScript.toAbsolutePath().toString(),
                        String.valueOf(currentPid),
                        packageFile.toAbsolutePath().toString(),
                        installDir.toAbsolutePath().toString(),
                        logFile.toAbsolutePath().toString()
                );
            } else if (name.endsWith(".pkg")) {
                builder = new ProcessBuilder("open", "-W", packageFile.toAbsolutePath().toString());
            } else {
                builder = new ProcessBuilder("open", "-R", packageFile.toAbsolutePath().toString());
            }
        } else {
            // Linux
            if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
                Path shScript = UPDATE_DIR.resolve("apply-update.sh");
                String scriptContent = """
                        #!/bin/bash
                        PID=$1
                        PKG="$2"
                        TARGET_DIR="$3"
                        LOG="$4"
                        
                        exec > "$LOG" 2>&1
                        echo "=== ThunderCall Linux In-Place Updater ==="
                        
                        if [ -n "$PID" ] && [ "$PID" -gt 0 ]; then
                            for i in {1..60}; do
                                if ! kill -0 "$PID" 2>/dev/null; then
                                    break
                                fi
                                sleep 0.5
                            done
                        fi
                        
                        TEMP_DIR=$(mktemp -d /tmp/thundercall_lin_upd.XXXXXX)
                        tar -xzf "$PKG" -C "$TEMP_DIR"
                        
                        SRC_DIR=$(find "$TEMP_DIR" -maxdepth 2 -name "bin" -type d | head -n 1)
                        if [ -n "$SRC_DIR" ]; then
                            SRC_DIR=$(dirname "$SRC_DIR")
                        else
                            SRC_DIR="$TEMP_DIR"
                        fi
                        
                        if [ -d "$TARGET_DIR" ] && [ -w "$TARGET_DIR" ]; then
                            cp -R "$SRC_DIR"/* "$TARGET_DIR/"
                            if [ -x "$TARGET_DIR/bin/thundercall" ]; then
                                nohup "$TARGET_DIR/bin/thundercall" >/dev/null 2>&1 &
                            fi
                        fi
                        
                        rm -rf "$TEMP_DIR"
                        rm -f "$PKG"
                        """;
                Files.writeString(shScript, scriptContent, java.nio.charset.StandardCharsets.UTF_8);
                shScript.toFile().setExecutable(true, false);

                builder = new ProcessBuilder(
                        "/bin/bash",
                        shScript.toAbsolutePath().toString(),
                        String.valueOf(currentPid),
                        packageFile.toAbsolutePath().toString(),
                        installDir.toAbsolutePath().toString(),
                        logFile.toAbsolutePath().toString()
                );
            } else if (name.endsWith(".deb")) {
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

        builder.directory(UPDATE_DIR.toFile());
        builder.start();

        javafx.application.Platform.exit();
        System.exit(0);
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
