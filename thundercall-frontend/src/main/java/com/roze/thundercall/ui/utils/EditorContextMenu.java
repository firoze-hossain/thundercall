package com.roze.thundercall.ui.utils;

import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.Clipboard;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Postman-style right-click menu for the request body / scripts editors:
 * Comment (opens an inline annotation thread — see CommentThreadPopup),
 * Set as variable, (Save to Package Library for scripts), Cut/Copy/Paste,
 * EncodeURIComponent/DecodeURIComponent, and Find. Ctrl+/ line-commenting
 * is a separate, always-available shortcut (see
 * MainController#attachCommentToggleShortcut) — not tied to this menu.
 * <p>
 * Works against any {@link TextEditTarget}, so the exact same menu logic
 * drives the raw/GraphQL body editors (RichTextFX CodeArea) and the
 * pre-request/tests script editors (plain TextArea) — replacing the
 * platform's default text-field context menu on both.
 */
public final class EditorContextMenu {

    private EditorContextMenu() {
    }

    /** App-specific actions this generic utility doesn't own (they need
     * access to the active environment / local package library / the
     * inline comment-thread backend). */
    public interface Extensions {
        void onSetAsVariable(TextEditTarget target);

        /** Postman's real inline-comment/annotation feature — opens a
         * composer for a new thread anchored to the current selection.
         * No-op by default since only the body editor wires this up. */
        default void onComment(TextEditTarget target) {
        }

        default void onSaveToExistingPackage(TextEditTarget target) {
        }

        default void onSaveToNewPackage(TextEditTarget target) {
        }
    }

    /**
     * Replaces the control's default context menu with the Postman-style one.
     * <p>
     * For a {@link Control} (e.g. TextArea) this uses {@code setContextMenu}
     * so the platform's own default menu (Undo/Cut/Copy/Paste/...) is fully
     * replaced rather than possibly firing alongside ours. RichTextFX's
     * {@code CodeArea} isn't a Control and has no default context menu of
     * its own, so it's shown via a context-menu-requested handler instead.
     * Either way the SAME persistent menu is reused and its dynamic bits
     * (disabled states, the "Find: ..." label) are refreshed on every
     * {@code onShowing} — which fires whichever way the menu is triggered.
     *
     * @param includeComment        show the "Comment" item — opens the inline
     *                              annotation-thread composer (body editor only)
     * @param includePackageLibrary show "Save to Package Library" (script editors)
     */
    public static void attach(TextEditTarget target, boolean includeComment,
                               boolean includePackageLibrary, Extensions extensions) {
        ContextMenu menu = build(target, includeComment, includePackageLibrary, extensions);
        Node node = target.getNode();
        if (node instanceof Control control) {
            control.setContextMenu(menu);
        } else {
            node.setOnContextMenuRequested(event -> {
                menu.show(node, event.getScreenX(), event.getScreenY());
                event.consume();
            });
        }
    }

    private static ContextMenu build(TextEditTarget target, boolean includeComment,
                                      boolean includePackageLibrary, Extensions extensions) {
        ContextMenu menu = new ContextMenu();

        if (includeComment) {
            MenuItem comment = new MenuItem("Comment");
            comment.setOnAction(e -> extensions.onComment(target));
            menu.getItems().add(comment);
        }

        MenuItem setVariable = new MenuItem("Set as variable");
        setVariable.setOnAction(e -> extensions.onSetAsVariable(target));
        menu.getItems().add(setVariable);

        MenuItem existingPackage = null;
        MenuItem newPackage = null;
        if (includePackageLibrary) {
            Menu packageMenu = new Menu("Save to Package Library");
            existingPackage = new MenuItem("Existing Package");
            existingPackage.setOnAction(e -> extensions.onSaveToExistingPackage(target));
            newPackage = new MenuItem("New Package");
            newPackage.setOnAction(e -> extensions.onSaveToNewPackage(target));
            packageMenu.getItems().addAll(existingPackage, newPackage);
            menu.getItems().add(packageMenu);
        }

        menu.getItems().add(new SeparatorMenuItem());

        MenuItem cut = new MenuItem("Cut");
        cut.setOnAction(e -> target.cut());

        MenuItem copy = new MenuItem("Copy");
        copy.setOnAction(e -> target.copy());

        MenuItem paste = new MenuItem("Paste");
        paste.setOnAction(e -> target.paste());

        menu.getItems().addAll(cut, copy, paste);
        menu.getItems().add(new SeparatorMenuItem());

        MenuItem encode = new MenuItem("EncodeURIComponent");
        encode.setOnAction(e -> replaceSelection(target, encodeURIComponent(target.getSelectedText())));

        MenuItem decode = new MenuItem("DecodeURIComponent");
        decode.setOnAction(e -> replaceSelection(target, decodeURIComponent(target.getSelectedText())));

        menu.getItems().addAll(encode, decode);

        MenuItem find = new MenuItem("Find");
        find.setOnAction(e -> {
            String sel = target.getSelectedText();
            boolean hasSel = sel != null && !sel.isEmpty();
            InlineFindPopup.showFor(target, hasSel ? sel : "");
        });
        menu.getItems().add(find);

        // Refreshed every time the menu is about to appear, regardless of
        // whether it was triggered via setContextMenu's automatic right-click
        // handling (TextArea) or our own explicit .show() call (CodeArea).
        MenuItem finalExistingPackage = existingPackage;
        MenuItem finalNewPackage = newPackage;
        menu.setOnShowing(e -> {
            String selected = target.getSelectedText();
            boolean hasSelection = selected != null && !selected.isEmpty();

            setVariable.setDisable(!hasSelection);
            if (finalExistingPackage != null) {
                finalExistingPackage.setDisable(!hasSelection);
            }
            if (finalNewPackage != null) {
                finalNewPackage.setDisable(!hasSelection);
            }
            cut.setDisable(!hasSelection);
            copy.setDisable(!hasSelection);
            paste.setDisable(!Clipboard.getSystemClipboard().hasString());
            encode.setDisable(!hasSelection);
            decode.setDisable(!hasSelection);
            find.setText("Find: " + truncate(hasSelection ? selected : target.getText()));
        });

        return menu;
    }

    private static void replaceSelection(TextEditTarget target, String replacement) {
        IndexRange range = target.getSelection();
        if (range == null) {
            return;
        }
        target.replaceText(range.getStart(), range.getEnd(), replacement);
    }

    /** Toggles "// " line comments across the selected lines (or the caret's
     * current line when nothing is selected) — Postman's Ctrl+/ shortcut,
     * independent of the "Comment" annotation-thread menu item above. */
    public static void toggleLineComment(TextEditTarget target) {
        String text = target.getText();
        if (text == null || text.isEmpty()) {
            return;
        }
        IndexRange sel = target.getSelection();
        int start = sel != null ? sel.getStart() : 0;
        int end = sel != null ? sel.getEnd() : 0;

        int lineStart = text.lastIndexOf('\n', Math.max(0, start - 1)) + 1;
        int searchFrom = Math.max(lineStart, end - 1);
        int lineEnd = text.indexOf('\n', searchFrom);
        if (lineEnd < 0) {
            lineEnd = text.length();
        }

        String block = text.substring(lineStart, lineEnd);
        String[] lines = block.split("\n", -1);

        boolean allCommented = true;
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("//")) {
                allCommented = false;
                break;
            }
        }

        StringBuilder rebuilt = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (allCommented) {
                rebuilt.append(line.replaceFirst("^(\\s*)//\\s?", "$1"));
            } else if (line.isEmpty()) {
                rebuilt.append(line);
            } else {
                rebuilt.append("// ").append(line);
            }
            if (i < lines.length - 1) {
                rebuilt.append("\n");
            }
        }

        target.replaceText(lineStart, lineEnd, rebuilt.toString());
    }

    private static String truncate(String s) {
        if (s == null) {
            return "";
        }
        String flat = s.replace("\n", " ").replace("\r", "").trim();
        return flat.length() > 28 ? flat.substring(0, 28) + "..." : flat;
    }

    /** Matches JavaScript's {@code encodeURIComponent}: percent-encodes
     * everything except {@code A-Za-z0-9 - _ . ! ~ * ' ( )}. */
    public static String encodeURIComponent(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : s.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xFF;
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.' || c == '!' || c == '~'
                    || c == '*' || c == '\'' || c == '(' || c == ')') {
                sb.append((char) c);
            } else {
                sb.append('%').append(String.format("%02X", c));
            }
        }
        return sb.toString();
    }

    /** Matches JavaScript's {@code decodeURIComponent} — unlike form
     * decoding, a literal '+' is NOT treated as a space. */
    public static String decodeURIComponent(String s) {
        if (s == null) {
            return "";
        }
        try {
            return URLDecoder.decode(s.replace("+", "%2B"), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }
}
