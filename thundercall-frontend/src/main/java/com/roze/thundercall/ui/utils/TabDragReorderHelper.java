package com.roze.thundercall.ui.utils;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.util.function.Consumer;

/**
 * Enables smooth, dynamic Postman-style drag-and-drop tab reordering for JavaFX TabPane.
 *
 * Features:
 * 1. Live, dynamic swapping: tabs reorder in real time as the user drags horizontally
 *    (both left-to-right and right-to-left) across neighboring tab centers.
 * 2. Visual feedback: the dragged tab is highlighted with subtle opacity and a grabbing cursor.
 * 3. Non-destructive: protects tab selection and editor state so unsaved changes, dirty dots,
 *    method badges, and focus remain completely undisturbed.
 * 4. Close-button safe: clicking the 'x' close button never triggers a drag.
 */
public final class TabDragReorderHelper {

    private static final String DRAG_KEY = "thundercall-tab-drag-installed";

    private final TabPane tabPane;
    private final Consumer<Boolean> movingStateConsumer;

    private Tab activeDraggedTab = null;
    private boolean isDragging = false;
    private double startScreenX = 0;
    private double startScreenY = 0;
    private long lastSwapTime = 0;

    private TabDragReorderHelper(TabPane tabPane, Consumer<Boolean> movingStateConsumer) {
        this.tabPane = tabPane;
        this.movingStateConsumer = movingStateConsumer;
        init();
    }

    public static TabDragReorderHelper install(TabPane tabPane, Consumer<Boolean> movingStateConsumer) {
        if (tabPane == null) {
            return null;
        }
        return new TabDragReorderHelper(tabPane, movingStateConsumer);
    }

    private void init() {
        for (Tab tab : tabPane.getTabs()) {
            setupTab(tab);
        }

        tabPane.getTabs().addListener((ListChangeListener<Tab>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (Tab tab : c.getAddedSubList()) {
                        setupTab(tab);
                    }
                }
            }
        });
    }

    public void setupTab(Tab tab) {
        if (tab == null) {
            return;
        }

        if (tab.getGraphic() != null) {
            attachToNode(tab, tab.getGraphic());
        }

        tab.graphicProperty().addListener((obs, oldG, newG) -> {
            if (newG != null) {
                attachToNode(tab, newG);
            }
        });

        Platform.runLater(() -> {
            Node header = getTabHeaderNode(tab);
            if (header != null) {
                attachToNode(tab, header);
            }
        });
    }

    private void attachToNode(Tab tab, Node node) {
        if (node == null) {
            return;
        }
        if (Boolean.TRUE.equals(node.getProperties().get(DRAG_KEY))) {
            return;
        }
        node.getProperties().put(DRAG_KEY, Boolean.TRUE);

        // Make child labels/badges in graphic mouseTransparent so parent node catches all drag events
        if (node instanceof Parent parent && !(node.getStyleClass().contains("tab"))) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                child.setMouseTransparent(true);
            }
            parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) c -> {
                while (c.next()) {
                    if (c.wasAdded()) {
                        for (Node child : c.getAddedSubList()) {
                            child.setMouseTransparent(true);
                        }
                    }
                }
            });
        }

        node.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() != MouseButton.PRIMARY) {
                return;
            }
            if (isCloseButton(e.getTarget())) {
                return;
            }

            activeDraggedTab = tab;
            startScreenX = e.getScreenX();
            startScreenY = e.getScreenY();
            isDragging = false;
            lastSwapTime = System.currentTimeMillis();

            if (tabPane.getSelectionModel().getSelectedItem() != tab) {
                tabPane.getSelectionModel().select(tab);
            }
        });

        node.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (e.getButton() != MouseButton.PRIMARY || activeDraggedTab != tab) {
                return;
            }
            if (isCloseButton(e.getTarget())) {
                return;
            }

            double deltaX = e.getScreenX() - startScreenX;
            double deltaY = e.getScreenY() - startScreenY;

            if (!isDragging) {
                if (Math.abs(deltaX) > 6 || Math.abs(deltaY) > 8) {
                    isDragging = true;
                    applyDragStyle(tab, true);
                }
            }

            if (isDragging) {
                long now = System.currentTimeMillis();
                if (now - lastSwapTime < 35) {
                    return;
                }

                double mouseX = e.getScreenX();
                ObservableList<Tab> tabs = tabPane.getTabs();
                int currentIdx = tabs.indexOf(tab);
                if (currentIdx < 0) {
                    return;
                }

                // Dragging to the left
                while (currentIdx > 0) {
                    Tab leftTab = tabs.get(currentIdx - 1);
                    Bounds leftBounds = getTabBounds(leftTab);
                    if (leftBounds != null && mouseX < leftBounds.getCenterX()) {
                        moveTab(currentIdx, currentIdx - 1);
                        currentIdx--;
                        lastSwapTime = now;
                    } else {
                        break;
                    }
                }

                // Dragging to the right
                while (currentIdx < tabs.size() - 1) {
                    Tab rightTab = tabs.get(currentIdx + 1);
                    Bounds rightBounds = getTabBounds(rightTab);
                    if (rightBounds != null && mouseX > rightBounds.getCenterX()) {
                        moveTab(currentIdx, currentIdx + 1);
                        currentIdx++;
                        lastSwapTime = now;
                    } else {
                        break;
                    }
                }
            }
        });

        node.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (activeDraggedTab == tab) {
                if (isDragging) {
                    applyDragStyle(tab, false);
                    isDragging = false;
                    if (tabPane.getSelectionModel().getSelectedItem() != tab) {
                        tabPane.getSelectionModel().select(tab);
                    }
                    e.consume();
                }
                activeDraggedTab = null;
            }
        });
    }

    private void moveTab(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) {
            return;
        }
        ObservableList<Tab> tabs = tabPane.getTabs();
        if (fromIndex < 0 || fromIndex >= tabs.size()) {
            return;
        }
        if (toIndex < 0 || toIndex >= tabs.size()) {
            return;
        }

        Tab tab = tabs.get(fromIndex);
        if (movingStateConsumer != null) {
            movingStateConsumer.accept(true);
        }
        try {
            tabs.remove(fromIndex);
            tabs.add(toIndex, tab);
            tabPane.getSelectionModel().select(tab);
        } finally {
            if (movingStateConsumer != null) {
                movingStateConsumer.accept(false);
            }
        }
    }

    private void applyDragStyle(Tab tab, boolean dragging) {
        if (tab == null) {
            return;
        }
        Node graphic = tab.getGraphic();
        if (graphic != null) {
            if (dragging) {
                if (!graphic.getStyleClass().contains("tab-graphic-dragging")) {
                    graphic.getStyleClass().add("tab-graphic-dragging");
                }
                graphic.setCursor(Cursor.CLOSED_HAND);
            } else {
                graphic.getStyleClass().remove("tab-graphic-dragging");
                graphic.setCursor(Cursor.HAND);
            }
        }
        Node header = getTabHeaderNode(tab);
        if (header != null) {
            if (dragging) {
                if (!header.getStyleClass().contains("tab-dragging")) {
                    header.getStyleClass().add("tab-dragging");
                }
                header.setCursor(Cursor.CLOSED_HAND);
            } else {
                header.getStyleClass().remove("tab-dragging");
                header.setCursor(Cursor.DEFAULT);
            }
        }
    }

    private Node getTabHeaderNode(Tab tab) {
        if (tab == null) {
            return null;
        }
        Node g = tab.getGraphic();
        if (g != null && g.getParent() != null) {
            Node p = g.getParent();
            if (p.getParent() != null && p.getParent().getStyleClass().contains("tab")) {
                return p.getParent();
            }
            if (p.getStyleClass().contains("tab")) {
                return p;
            }
        }
        if (tab.getTabPane() != null) {
            for (Node n : tab.getTabPane().lookupAll(".tab")) {
                if (n instanceof Parent parent && containsNode(parent, g)) {
                    return n;
                }
            }
        }
        return g;
    }

    private boolean containsNode(Parent parent, Node target) {
        if (parent == null || target == null) {
            return false;
        }
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child == target) {
                return true;
            }
            if (child instanceof Parent p && containsNode(p, target)) {
                return true;
            }
        }
        return false;
    }

    private Bounds getTabBounds(Tab tab) {
        if (tab == null) {
            return null;
        }
        Node header = getTabHeaderNode(tab);
        if (header == null) {
            header = tab.getGraphic();
        }
        if (header != null && header.getScene() != null) {
            return header.localToScreen(header.getBoundsInLocal());
        }
        return null;
    }

    private boolean isCloseButton(Object target) {
        if (target instanceof Node node) {
            Node curr = node;
            while (curr != null) {
                if (curr.getStyleClass().contains("tab-close-button")) {
                    return true;
                }
                curr = curr.getParent();
            }
        }
        return false;
    }
}
