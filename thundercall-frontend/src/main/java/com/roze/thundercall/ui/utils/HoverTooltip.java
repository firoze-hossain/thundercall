package com.roze.thundercall.ui.utils;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

/**
 * Provides zero-flicker, persistent hover tooltips across the application.
 *
 * Under Linux GTK, JavaFX's built-in TooltipBehavior positions tooltips directly
 * beneath the mouse pointer or captures focus, which triggers synthetic MOUSE_EXITED
 * events on the host node, causing infinite flickering (rapid show/hide cycles) or
 * dismissing after 5 seconds before the user can read the tooltip.
 *
 * This utility:
 * 1. Detaches default JavaFX TooltipBehavior completely (node.setTooltip(null)).
 * 2. Positions the tooltip cleanly offset (4px) below the host node's bounds so
 *    it never intersects or captures the mouse cursor.
 * 3. Keeps the tooltip visible continuously as long as the cursor hovers over the node
 *    (or over the tooltip card itself), with no arbitrary 5-second timeout.
 * 4. Filters out false-alarm exit events where the mouse is still physically within
 *    the node's screen bounds (e.g. crossing over child graphics).
 * 5. Applies active theme stylesheets dynamically (via ThemeManager) to prevent black boxes.
 * 6. Dismisses cleanly and smoothly when the cursor leaves the node, or immediately
 *    when the node is clicked or the parent window loses focus.
 */
public final class HoverTooltip {

    private static final String INSTALLED_KEY = "hover-tooltip-installed";

    private HoverTooltip() {
    }

    /**
     * Attaches a zero-flicker, persistent hover tooltip to the given node.
     * Safe to call on any Node (Button, Label, Region, etc.).
     */
    public static void attach(Node node, String text) {
        if (node == null || text == null || text.isBlank()) {
            return;
        }

        // Prevent duplicate installations on the same node
        if (Boolean.TRUE.equals(node.getProperties().get(INSTALLED_KEY))) {
            return;
        }
        node.getProperties().put(INSTALLED_KEY, Boolean.TRUE);

        // Detach default TooltipBehavior if it's a Control
        if (node instanceof Control control) {
            control.setTooltip(null);
        }

        // Ensure graphics inside buttons/labels don't swallow mouse events
        if (node instanceof Labeled labeled) {
            if (labeled.getGraphic() != null) {
                labeled.getGraphic().setMouseTransparent(true);
            }
            labeled.graphicProperty().addListener((obs, oldG, newG) -> {
                if (newG != null) {
                    newG.setMouseTransparent(true);
                }
            });
        }

        Tooltip tip = new Tooltip(text);
        tip.getStyleClass().add("custom-hover-tooltip");
        tip.setAutoHide(false);

        PauseTransition showDelay = new PauseTransition(Duration.millis(80));
        PauseTransition hideDelay = new PauseTransition(Duration.millis(120));

        showDelay.setOnFinished(e -> {
            if (!node.isVisible() || node.getScene() == null) {
                return;
            }
            Bounds bounds = node.localToScreen(node.getBoundsInLocal());
            if (bounds != null) {
                double tipWidth = tip.getWidth() > 0 ? tip.getWidth() : (text.length() * 7.0 + 16.0);
                double x = Math.max(8.0, bounds.getMinX() + (bounds.getWidth() - tipWidth) / 2.0);
                double y = bounds.getMaxY() + 4.0;

                // If tooltip would extend off the bottom of the window/screen, show above node
                if (node.getScene() != null && node.getScene().getWindow() != null) {
                    javafx.stage.Window win = node.getScene().getWindow();
                    double bottomBoundary = win.getY() + win.getHeight();
                    if (y + 36.0 > bottomBoundary) {
                        y = Math.max(8.0, bounds.getMinY() - 32.0);
                    }
                }

                if (!tip.isShowing()) {
                    tip.show(node, x, y);
                }
            }
        });

        hideDelay.setOnFinished(e -> {
            if (tip.isShowing()) {
                tip.hide();
            }
        });

        node.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            hideDelay.stop();
            if (!tip.isShowing()) {
                showDelay.playFromStart();
            }
        });

        node.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            hideDelay.stop();
            if (!tip.isShowing()) {
                showDelay.playFromStart();
            }
        });

        node.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            Point2D local = node.screenToLocal(e.getScreenX(), e.getScreenY());
            if (local != null && node.contains(local)) {
                // False alarm from GTK or child hierarchy: cursor is still physically within the node bounds
                return;
            }
            showDelay.stop();
            hideDelay.playFromStart();
        });

        node.addEventHandler(ActionEvent.ACTION, e -> {
            showDelay.stop();
            if (tip.isShowing()) {
                tip.hide();
            }
        });

        node.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            showDelay.stop();
            if (tip.isShowing()) {
                tip.hide();
            }
        });

        tip.setOnShowing(ev -> {
            if (tip.getScene() != null) {
                ThemeManager.apply(tip.getScene());
                if (tip.getScene().getRoot() != null) {
                    Node root = tip.getScene().getRoot();
                    root.setOnMouseEntered(me -> hideDelay.stop());
                    root.setOnMouseExited(me -> {
                        Point2D localRoot = root.screenToLocal(me.getScreenX(), me.getScreenY());
                        if (localRoot != null && root.contains(localRoot)) {
                            return;
                        }
                        Point2D localNode = node.screenToLocal(me.getScreenX(), me.getScreenY());
                        if (localNode != null && node.contains(localNode)) {
                            return;
                        }
                        hideDelay.playFromStart();
                    });
                }
            }
        });

        tip.setOnShown(ev -> {
            Bounds b2 = node.localToScreen(node.getBoundsInLocal());
            if (b2 != null && tip.isShowing()) {
                double actualW = tip.getWidth();
                double targetX = Math.max(8.0, b2.getMinX() + (b2.getWidth() - actualW) / 2.0);
                tip.setX(targetX);
            }
        });

        node.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                showDelay.stop();
                hideDelay.stop();
                if (tip.isShowing()) {
                    tip.hide();
                }
            } else {
                newScene.windowProperty().addListener((wObs, oldWin, newWin) -> {
                    if (newWin != null) {
                        newWin.focusedProperty().addListener((fObs, oF, nF) -> {
                            if (!nF) {
                                showDelay.stop();
                                if (tip.isShowing()) {
                                    tip.hide();
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    /**
     * Recursively traverses a node hierarchy (such as the root BorderPane or any container),
     * finding all controls with default JavaFX Tooltips and converting them to zero-flicker,
     * theme-styled HoverTooltips.
     */
    public static void convertAll(Node root) {
        if (root == null) {
            return;
        }

        if (root instanceof Control control) {
            Tooltip oldTip = control.getTooltip();
            if (oldTip != null) {
                String text = oldTip.getText();
                control.setTooltip(null);
                attach(control, text);
            }
            if (control instanceof TabPane tabPane) {
                for (Tab tab : tabPane.getTabs()) {
                    if (tab.getContent() != null) {
                        convertAll(tab.getContent());
                    }
                }
            }
        }

        if (root instanceof SplitPane splitPane) {
            for (Node item : splitPane.getItems()) {
                convertAll(item);
            }
        }

        if (root instanceof ScrollPane scrollPane) {
            if (scrollPane.getContent() != null) {
                convertAll(scrollPane.getContent());
            }
        }

        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                convertAll(child);
            }
        }
    }
}
