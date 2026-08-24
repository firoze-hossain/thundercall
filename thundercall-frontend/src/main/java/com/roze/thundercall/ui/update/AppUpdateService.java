package com.roze.thundercall.ui.update;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.prefs.Preferences;

/** RozeHub application update client for Thundercall. */
public final class AppUpdateService {
    public static final String PRODUCT_SLUG = "thundercall";
    public static final String DEFAULT_ENDPOINT = "http://127.0.0.1:8000/api/v1/updates/thundercall";
    public static final String CURRENT_VERSION = "1.0.0";
    private static final String PREF_AUTO_CHECK = "updates.autoCheck";
    private static final String PREF_CHANNEL = "updates.channel";
    private static final Preferences PREFS = Preferences.userNodeForPackage(AppUpdateService.class);
    private static final HttpClient HTTP = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    private AppUpdateService() {}

    public static boolean isAutoCheckEnabled() { return PREFS.getBoolean(PREF_AUTO_CHECK, true); }
    public static void setAutoCheckEnabled(boolean enabled) { PREFS.putBoolean(PREF_AUTO_CHECK, enabled); }
    public static String getChannel() { return PREFS.get(PREF_CHANNEL, "Stable"); }
    public static void setChannel(String channel) { PREFS.put(PREF_CHANNEL, channel == null ? "Stable" : channel); }
    public static String getEndpoint() { return PREFS.get("updates.endpoint", DEFAULT_ENDPOINT); }
    public static void setEndpoint(String endpoint) { PREFS.put("updates.endpoint", endpoint == null || endpoint.isBlank() ? DEFAULT_ENDPOINT : endpoint.trim()); }

    public static String platform() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) return "Windows";
        if (os.contains("mac")) return "macOS";
        if (os.contains("linux")) return "Linux";
        return "Other";
    }

    public static String architecture() {
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (arch.contains("aarch64") || arch.contains("arm64")) return "ARM64";
        if (arch.contains("x86_64") || arch.contains("amd64") || arch.equals("x64")) return "x64";
        return arch;
    }

    public static AppUpdate checkForUpdate() throws IOException, InterruptedException {
        String endpoint = getEndpoint();
        String separator = endpoint.contains("?") ? "&" : "?";
        String url = endpoint + separator + "version=" + enc(CURRENT_VERSION)
                + "&platform=" + enc(platform())
                + "&architecture=" + enc(architecture())
                + "&channel=" + enc(getChannel());
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("RozeHub update server returned HTTP " + response.statusCode());
        }
        return parse(response.body());
    }

    private static AppUpdate parse(String body) {
        JSONObject root = new JSONObject(body);
        JSONObject release = root.optJSONObject("release");
        JSONObject updateArtifact = root.optJSONObject("updateArtifact");
        JSONObject installerArtifact = root.optJSONObject("installerArtifact");
        if (release != null) {
            updateArtifact = firstNonNull(updateArtifact, release.optJSONObject("updateArtifact"));
            installerArtifact = firstNonNull(installerArtifact, release.optJSONObject("installerArtifact"));
        }
        return new AppUpdate(
                root.optString("project", PRODUCT_SLUG),
                root.optString("currentVersion", CURRENT_VERSION),
                root.optString("latestVersion", release == null ? CURRENT_VERSION : release.optString("version", CURRENT_VERSION)),
                root.optBoolean("available", root.optBoolean("updateAvailable", false)),
                root.optBoolean("mandatory", false),
                root.optString("minimumSupportedVersion", null),
                artifact(updateArtifact),
                artifact(installerArtifact),
                release == null ? root.optString("releaseNotes", "") : release.optString("releaseNotes", release.optString("notes", ""))
        );
    }

    private static JSONObject firstNonNull(JSONObject a, JSONObject b) { return a != null ? a : b; }

    private static AppUpdate.ReleaseArtifact artifact(JSONObject o) {
        if (o == null) return null;
        return new AppUpdate.ReleaseArtifact(
                o.optLong("id", 0), o.optString("purpose", ""), o.optString("packageType", ""),
                o.optString("platform", platform()), o.optString("architecture", architecture()),
                o.optString("fileName", "update-package"), o.optLong("fileSize", 0),
                o.optString("sha256", ""), o.optString("downloadUrl", "")
        );
    }

    public static Path downloadAndVerify(AppUpdate.ReleaseArtifact artifact, DownloadProgress progress)
            throws IOException, InterruptedException {
        if (artifact == null || artifact.downloadUrl() == null || artifact.downloadUrl().isBlank()) {
            throw new IOException("RozeHub did not provide an update download URL.");
        }
        Path dir = Paths.get(System.getProperty("user.home"), ".thundercall", "updates");
        Files.createDirectories(dir);
        String safeName = sanitize(artifact.fileName());
        Path part = dir.resolve(safeName + ".part");
        Path target = dir.resolve(safeName);
        HttpRequest request = HttpRequest.newBuilder(URI.create(artifact.downloadUrl())).GET().build();
        HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IOException("Download failed: HTTP " + response.statusCode());
        long expected = artifact.fileSize();
        long total = 0;
        try (InputStream in = response.body(); OutputStream out = Files.newOutputStream(part, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            byte[] buffer = new byte[1024 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read); total += read;
                if (progress != null) progress.onProgress(total, expected);
            }
        } catch (Exception e) {
            Files.deleteIfExists(part); throw e;
        }
        if (expected > 0 && total != expected) { Files.deleteIfExists(part); throw new IOException("Downloaded size does not match RozeHub metadata."); }
        if (!artifact.sha256().isBlank()) {
            String actual = sha256(part);
            if (!actual.equalsIgnoreCase(artifact.sha256())) { Files.deleteIfExists(part); throw new IOException("SHA-256 verification failed."); }
        }
        Files.move(part, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return target;
    }

    /** Starts a detached helper, then exits the JavaFX process. */
    public static void installAndRestart(Path packageFile, AppUpdate.ReleaseArtifact artifact) throws IOException {
        Path updaterDir = Paths.get(System.getProperty("user.home"), ".thundercall", "updates");
        Files.createDirectories(updaterDir);
        String pkg = artifact.packageType().toLowerCase(Locale.ROOT);
        Path script = platformScript(updaterDir, packageFile, pkg);
        ProcessBuilder pb;
        if (platform().equals("Windows")) pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", script.toString());
        else pb = new ProcessBuilder("sh", script.toString());
        pb.directory(updaterDir.toFile());
        pb.start();
        javafx.application.Platform.exit();
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
                case "deb" -> "pkexec apt-get install -y \"$PACKAGE\"";
                case "pkg" -> "sudo installer -pkg \"$PACKAGE\" -target /";
                case "dmg" -> "open \"$PACKAGE\"";
                case "appimage" -> "chmod +x \"$PACKAGE\" && \"$PACKAGE\" &";
                default -> "open \"$PACKAGE\"";
            };
            String text = "#!/bin/sh\nPACKAGE=\"" + sh(packageFile.toAbsolutePath()) + "\"\n"
                    + "while kill -0 " + pid + " 2>/dev/null; do sleep 0.3; done\n"
                    + install + "\n"
                    + "sleep 2\n\"" + sh(Path.of(appCommand)) + "\" >/dev/null 2>&1 &\n";
            Files.writeString(script, text, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            script.toFile().setExecutable(true);
        }
        return script;
    }

    private static String sha256(Path file) throws IOException {
        try { MessageDigest md = MessageDigest.getInstance("SHA-256"); try (InputStream in = Files.newInputStream(file)) { byte[] b = new byte[1024 * 1024]; int n; while ((n=in.read(b))!=-1) md.update(b,0,n); } StringBuilder s=new StringBuilder(); for(byte x:md.digest()) s.append(String.format("%02x",x)); return s.toString(); }
        catch (Exception e) { throw new IOException("Unable to calculate SHA-256", e); }
    }
    private static String sanitize(String n) { return n.replaceAll("[^a-zA-Z0-9._-]", "_"); }
    private static String enc(String s) { return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8); }
    private static String ps(Path p) { return p.toString().replace("'", "''"); }
    private static String sh(Path p) { return p.toString().replace("\"", "\\\""); }
    public interface DownloadProgress { void onProgress(long downloaded, long total); }
}
