package com.roze.thundercall.ui.services;

import com.roze.thundercall.ui.Main;

public class DeepLinkService {
    public static void openBrowserForAuth() {
        String authUrl = "http://localhost:8084/api/v1/deeplink/auth?platform=" + PlatformService.getPlatFormName();
        Main.getAppHostServices().showDocument(authUrl);
    }
}
