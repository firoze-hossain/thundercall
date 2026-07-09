package com.roze.thundercall.services;

public class PlatformService {
    public static String getPlatFormName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "windows";
        }
        if (os.contains("mac")) {
            return "mac";
        }
        if (os.contains("nix") || os.contains("nux")) {
            return "linux";
        }
        return "desktop";
    }
}
