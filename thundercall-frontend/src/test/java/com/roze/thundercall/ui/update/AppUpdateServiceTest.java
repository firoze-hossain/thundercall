package com.roze.thundercall.ui.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AppUpdateServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testVersionComparison() {
        assertTrue(AppUpdateService.isNewerVersion("1.0.1", "1.0.0"));
        assertTrue(AppUpdateService.isNewerVersion("1.10.0", "1.9.0"));
        assertTrue(AppUpdateService.isNewerVersion("2.0.0", "1.99.99"));
        assertTrue(AppUpdateService.isNewerVersion("v1.0.1", "1.0.0"));
        assertTrue(AppUpdateService.isNewerVersion("1.0.1", "v1.0.0"));

        assertFalse(AppUpdateService.isNewerVersion("1.0.0", "1.0.0"));
        assertFalse(AppUpdateService.isNewerVersion("v1.0.0", "1.0.0"));
        assertFalse(AppUpdateService.isNewerVersion("1.0.0", "1.0.1"));
        assertFalse(AppUpdateService.isNewerVersion("1.0.0", "2.0.0"));
        assertEquals(0, AppUpdateService.compareVersions("1.0.0", "1.0.0"));
        assertEquals(0, AppUpdateService.compareVersions("v1.0.0", "1.0.0"));
    }

    @Test
    void testParseGitHubReleaseJson() throws Exception {
        String json = """
        {
          "tag_name": "v1.1.0",
          "name": "ThunderCall 1.1.0",
          "body": "## Features\\n- Added new request collection export\\n- Improved performance",
          "draft": false,
          "assets": [
            {
              "name": "thundercall_1.1.0_amd64.deb",
              "size": 98567200,
              "browser_download_url": "https://github.com/firoze-hossain/thundercall/releases/download/v1.1.0/thundercall_1.1.0_amd64.deb"
            },
            {
              "name": "ThunderCall-Setup-1.1.0.exe",
              "size": 105200300,
              "browser_download_url": "https://github.com/firoze-hossain/thundercall/releases/download/v1.1.0/ThunderCall-Setup-1.1.0.exe"
            },
            {
              "name": "ThunderCall-1.1.0.dmg",
              "size": 112000000,
              "browser_download_url": "https://github.com/firoze-hossain/thundercall/releases/download/v1.1.0/ThunderCall-1.1.0.dmg"
            }
          ]
        }
        """;

        JsonNode root = mapper.readTree(json);
        AppUpdate update = AppUpdateService.parseGitHubRelease(root, "1.0.0", "Stable", "firoze-hossain/thundercall");

        assertNotNull(update);
        assertTrue(update.available());
        assertEquals("1.1.0", update.latestVersion());
        assertEquals("github", update.source);
        assertNotNull(update.release);
        assertEquals("1.1.0", update.release.version);
        assertTrue(update.release.displayNotes().contains("Added new request collection export"));

        // Check asset selection matching current OS
        String os = AppUpdateService.platform();
        if ("Linux".equals(os)) {
            assertEquals("thundercall_1.1.0_amd64.deb", update.release.effectiveFileName());
            assertTrue(update.release.effectiveDownloadUrl().endsWith(".deb"));
        } else if ("Windows".equals(os)) {
            assertEquals("ThunderCall-Setup-1.1.0.exe", update.release.effectiveFileName());
            assertTrue(update.release.effectiveDownloadUrl().endsWith(".exe"));
        } else if ("macOS".equals(os)) {
            assertEquals("ThunderCall-1.1.0.dmg", update.release.effectiveFileName());
            assertTrue(update.release.effectiveDownloadUrl().endsWith(".dmg"));
        }
    }

    @Test
    void testUpdatePackagePreferredOverFullInstaller() throws Exception {
        String json = """
        {
          "tag_name": "v1.0.1",
          "name": "ThunderCall 1.0.1",
          "body": "In-place seamless updates",
          "published_at": "2026-10-31T10:00:00Z",
          "html_url": "https://github.com/firoze-hossain/thundercall/releases/tag/v1.0.1",
          "assets": [
            {
              "name": "thundercall_1.0.1_amd64.deb",
              "size": 84000000,
              "browser_download_url": "https://github.com/firoze-hossain/thundercall/releases/download/v1.0.1/thundercall_1.0.1_amd64.deb"
            },
            {
              "name": "thundercall-1.0.1-linux-update.tar.gz",
              "size": 75000000,
              "browser_download_url": "https://github.com/firoze-hossain/thundercall/releases/download/v1.0.1/thundercall-1.0.1-linux-update.tar.gz"
            },
            {
              "name": "ThunderCall-Setup-1.0.1.exe",
              "size": 89000000,
              "browser_download_url": "https://github.com/firoze-hossain/thundercall/releases/download/v1.0.1/ThunderCall-Setup-1.0.1.exe"
            },
            {
              "name": "ThunderCall-1.0.1-windows-update.zip",
              "size": 78000000,
              "browser_download_url": "https://github.com/firoze-hossain/thundercall/releases/download/v1.0.1/ThunderCall-1.0.1-windows-update.zip"
            },
            {
              "name": "ThunderCall-1.0.1.dmg",
              "size": 95000000,
              "browser_download_url": "https://github.com/firoze-hossain/thundercall/releases/download/v1.0.1/ThunderCall-1.0.1.dmg"
            },
            {
              "name": "ThunderCall-1.0.1.pkg",
              "size": 93000000,
              "browser_download_url": "https://github.com/firoze-hossain/thundercall/releases/download/v1.0.1/ThunderCall-1.0.1.pkg"
            },
            {
              "name": "ThunderCall-1.0.1-macos-update.tar.gz",
              "size": 80000000,
              "browser_download_url": "https://github.com/firoze-hossain/thundercall/releases/download/v1.0.1/ThunderCall-1.0.1-macos-update.tar.gz"
            }
          ]
        }
        """;

        JsonNode releaseNode = mapper.readTree(json);
        AppUpdate update = AppUpdateService.parseGitHubRelease(releaseNode, "1.0.0", "Stable", "firoze-hossain/thundercall");

        assertNotNull(update);
        assertTrue(update.available());
        assertNotNull(update.release);

        String platform = AppUpdateService.platform();
        if ("Linux".equals(platform)) {
            assertEquals("thundercall-1.0.1-linux-update.tar.gz", update.release.effectiveFileName());
        } else if ("Windows".equals(platform)) {
            assertEquals("ThunderCall-1.0.1-windows-update.zip", update.release.effectiveFileName());
        } else if ("macOS".equals(platform)) {
            assertEquals("ThunderCall-1.0.1-macos-update.tar.gz", update.release.effectiveFileName());
        }
    }

    @Test
    void testGetInstallationDirectory() {
        assertNotNull(AppUpdateService.getInstallationDirectory());
    }

    @Test
    void testWebsiteFallbackWhenOffline() {
        // When checking against a non-existent or down endpoint, it must gracefully
        // fall back to GitHub without unhandled exceptions.
        AppUpdate update = AppUpdateService.checkForUpdateAsync("http://127.0.0.1:54321/api/v1/updates/invalid", "Stable", "firoze-hossain/thundercall")
                .join();

        assertNotNull(update);
        // It should complete gracefully (either finding nothing or github result)
        assertEquals("thundercall", update.project);
        assertEquals(AppUpdateService.currentVersion(), update.currentVersion());
    }

    @Test
    void testScoreAssetPlatformExclusion() {
        // macOS runner must NEVER pick Linux or Windows packages
        assertEquals(0, AppUpdateService.scoreAsset("thundercall-1.0.1-linux-update.tar.gz", "macOS", "x64"));
        assertEquals(0, AppUpdateService.scoreAsset("thundercall_1.0.1_amd64.deb", "macOS", "x64"));
        assertEquals(0, AppUpdateService.scoreAsset("ThunderCall-1.0.1-windows-update.zip", "macOS", "x64"));
        assertEquals(0, AppUpdateService.scoreAsset("ThunderCall-Setup-1.0.1.exe", "macOS", "x64"));
        assertTrue(AppUpdateService.scoreAsset("ThunderCall-1.0.1-macos-update.tar.gz", "macOS", "x64") >= 2000);
        assertTrue(AppUpdateService.scoreAsset("ThunderCall-1.0.1.dmg", "macOS", "x64") > 0);

        // Linux runner must NEVER pick macOS or Windows packages
        assertEquals(0, AppUpdateService.scoreAsset("ThunderCall-1.0.1-macos-update.tar.gz", "Linux", "x64"));
        assertEquals(0, AppUpdateService.scoreAsset("ThunderCall-1.0.1-windows-update.zip", "Linux", "x64"));
        assertEquals(0, AppUpdateService.scoreAsset("ThunderCall-Setup-1.0.1.exe", "Linux", "x64"));
        assertTrue(AppUpdateService.scoreAsset("thundercall-1.0.1-linux-update.tar.gz", "Linux", "x64") >= 2000);

        // Windows runner must NEVER pick Linux or macOS packages
        assertEquals(0, AppUpdateService.scoreAsset("thundercall-1.0.1-linux-update.tar.gz", "Windows", "x64"));
        assertEquals(0, AppUpdateService.scoreAsset("ThunderCall-1.0.1-macos-update.tar.gz", "Windows", "x64"));
        assertTrue(AppUpdateService.scoreAsset("ThunderCall-1.0.1-windows-update.zip", "Windows", "x64") >= 2000);
    }
}
