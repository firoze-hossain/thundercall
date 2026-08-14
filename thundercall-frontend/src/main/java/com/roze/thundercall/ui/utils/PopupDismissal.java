package com.roze.thundercall.ui.utils;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.PopupWindow;

/**
 * Belt-and-suspenders "click outside to close" for our Popup/ContextMenu
 * overlays — the Comment composer, the comment thread viewer, the
 * "Set as new variable" card, and the editor's right-click menu.
 * <p>
 * Every one of these already turns on the platform's own autoHide /
 * hideOnEscape flags, and Escape reliably works — but a genuine click
 * outside the popup doesn't always register as an "autohide" event on
 * every OS/window-manager combination. This adds an explicit fallback
 * that works everywhere: while the popup is open, ANY mouse press
 * anywhere in the anchor's own window closes it.
 * <p>
 * No bounds math is needed to tell "inside" from "outside" — a click that
 * actually lands on the popup's own content is delivered to the popup's
 * own separate window/scene and never reaches this filter at all, so
 * every press this filter sees is, by construction, outside the popup.
 * Escape still works exactly as before (untouched) and either path can
 * close the popup first; the other is simply a no-op afterward.
 */
public final class PopupDismissal {

    private PopupDismissal() {
    }

    /**
     * Installs the outside-click fallback for one showing of {@code popup}.
     * Safe to call every time the popup is (re)shown — its own filter is
     * removed automatically once the popup hides, so repeated show/hide
     * cycles (e.g. a reused {@code ContextMenu}) never accumulate filters.
     */
    public static void closeOnOutsideClick(PopupWindow popup, Node anchor) {
        Scene scene = anchor.getScene();
        if (scene == null) {
            return;
        }
        EventHandler<MouseEvent> outsideClickHandler = event -> {
            if (popup.isShowing()) {
                popup.hide();
            }
        };
        // Capturing-phase filter so it still fires no matter what the
        // click's actual target does with the event afterward.
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, outsideClickHandler);
        popup.setOnHidden(e -> scene.removeEventFilter(MouseEvent.MOUSE_PRESSED, outsideClickHandler));
    }
}
