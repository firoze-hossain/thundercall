package com.roze.thundercall.services;

import com.roze.thundercall.Main;

public class DeepLinkService {
    public static void openBrowserForAuth() {
        String authUrl = "http://localhost:8084/api/v1/deeplink/auth?platform=" + PlatformService.getPlatFormName();
        Main.getAppHostServices().showDocument(authUrl);
    }
}
