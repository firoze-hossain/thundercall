package com.roze.thundercall.ui.utils;

import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * Dark / Light theme switching, Postman-style.
 *
 * How it works: main.css defines all colors as looked-up colors on .root
 * (dark values). light.css re-defines ONLY those color tokens. Adding
 * light.css AFTER main.css on the root node overrides the palette while
 * keeping every layout rule. The choice is persisted with Preferences,
 * so the app reopens in the last chosen theme.
 */
public final class ThemeManager {

    private static final String PREF_KEY = "thundercall.theme";
    private static final String LIGHT = "light";
    private static final String DARK = "dark";

    private ThemeManager() {
    }

    public static boolean isLight() {
        return LIGHT.equals(prefs().get(PREF_KEY, DARK));
    }

    public static void setLight(boolean light, Scene scene) {
        prefs().put(PREF_KEY, light ? LIGHT : DARK);
        apply(scene);
    }

    /** Applies the persisted theme to the given scene. Safe to call any time. */
    public static void apply(Scene scene) {
        if (scene == null || scene.getRoot() == null) {
            return;
        }
        String lightCss = lightCss();
        scene.getRoot().getStylesheets().remove(lightCss);
        if (isLight()) {
            // Root stylesheets outrank scene stylesheets; appending after
            // main.css (declared in FXML) makes the light tokens win.
            scene.getRoot().getStylesheets().add(lightCss);
        }
    }

    /**
     * Dialogs and Alerts open in their own scene and do NOT inherit the app
     * stylesheet. Call this on every DialogPane (AlertUtils should call it
     * too) so popups match the current theme.
     */
    public static void styleDialog(DialogPane pane) {
        if (pane == null) {
            return;
        }
        String main = mainCss();
        if (!pane.getStylesheets().contains(main)) {
            pane.getStylesheets().add(main);
        }
        String lightCss = lightCss();
        pane.getStylesheets().remove(lightCss);
        if (isLight()) {
            pane.getStylesheets().add(lightCss);
        }
        anchorToMainWindow(pane);
    }

    /**
     * macOS FIX: dialogs and alerts created without an owner open as their
     * own top-level window. When the app is in macOS fullscreen mode, that
     * ownerless window gets promoted to its own fullscreen Space — so a
     * tiny "Profile feature coming soon!" info alert suddenly covers the
     * entire screen. Giving every dialog the primary stage as its owner
     * makes it appear as a normal-sized modal ON TOP of the app window
     * instead. Since every dialog in the app already routes through
     * styleDialog(), hooking it here fixes all of them at once.
     */
    private static void anchorToMainWindow(DialogPane pane) {
        if (pane.getScene() != null) {
            setOwner(pane.getScene().getWindow());
        } else {
            // Dialog implementations create their Scene lazily — attach as
            // soon as it exists, which is still before the dialog is shown.
            pane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    setOwner(newScene.getWindow());
                }
            });
        }
    }

    private static void setOwner(Window window) {
        Stage owner;
        try {
            owner = com.roze.thundercall.ui.Main.getPrimaryStage();
        } catch (Throwable t) {
            return; // e.g. running a controller in isolation in tests
        }
        if (!(window instanceof Stage stage) || owner == null || stage == owner) {
            return;
        }
        // initOwner throws if the dialog is already showing or already
        // owned — both mean there's nothing left to do.
        if (stage.isShowing() || stage.getOwner() != null) {
            return;
        }
        try {
            stage.initOwner(owner);
        } catch (IllegalStateException ignored) {
            // Shown between our check and the call — harmless.
        }
    }

    private static String mainCss() {
        return Objects.requireNonNull(
                ThemeManager.class.getResource("/css/main.css")).toExternalForm();
    }

    private static String lightCss() {
        return Objects.requireNonNull(
                ThemeManager.class.getResource("/css/light.css")).toExternalForm();
    }

    private static Preferences prefs() {
        return Preferences.userNodeForPackage(ThemeManager.class);
    }
}
