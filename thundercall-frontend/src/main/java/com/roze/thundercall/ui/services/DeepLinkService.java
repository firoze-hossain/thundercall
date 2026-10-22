package com.roze.thundercall.ui.services;

import com.roze.thundercall.ui.Main;

public class DeepLinkService {
    public static void openBrowserForAuth() {
        DesktopBrowserAuthService.startBrowserAuth(null, () -> Main.showMainView(), null);
    }
}
