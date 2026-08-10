package com.roze.thundercall.ui.utils;

import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.control.TextInputControl;
import org.fxmisc.richtext.CodeArea;

/**
 * A uniform text-editing surface so the same Postman-style right-click menu
 * (see {@link EditorContextMenu}) can drive BOTH RichTextFX's {@link CodeArea}
 * (used for the raw/GraphQL request body) and plain JavaFX {@link TextInputControl}
 * (used for the Pre-request/Tests scripts editors) without duplicating the
 * menu-building logic for each control type.
 */
public interface TextEditTarget {

    /** The underlying editor Node — used to anchor the context menu / find popup. */
    Node getNode();

    String getText();

    String getSelectedText();

    IndexRange getSelection();

    void selectRange(int start, int end);

    void replaceText(int start, int end, String text);

    void cut();

    void copy();

    void paste();

    /** Wraps a RichTextFX CodeArea (the JSON/GraphQL request body editors). */
    static TextEditTarget of(CodeArea area) {
        return new TextEditTarget() {
            @Override
            public Node getNode() {
                return area;
            }

            @Override
            public String getText() {
                return area.getText();
            }

            @Override
            public String getSelectedText() {
                return area.getSelectedText();
            }

            @Override
            public IndexRange getSelection() {
                return area.getSelection();
            }

            @Override
            public void selectRange(int start, int end) {
                area.selectRange(start, end);
            }

            @Override
            public void replaceText(int start, int end, String text) {
                area.replaceText(start, end, text);
            }

            @Override
            public void cut() {
                area.cut();
            }

            @Override
            public void copy() {
                area.copy();
            }

            @Override
            public void paste() {
                area.paste();
            }
        };
    }

    /** Wraps a plain JavaFX text control (the Pre-request/Tests script editors). */
    static TextEditTarget of(TextInputControl area) {
        return new TextEditTarget() {
            @Override
            public Node getNode() {
                return area;
            }

            @Override
            public String getText() {
                return area.getText();
            }

            @Override
            public String getSelectedText() {
                return area.getSelectedText();
            }

            @Override
            public IndexRange getSelection() {
                return area.getSelection();
            }

            @Override
            public void selectRange(int start, int end) {
                area.selectRange(start, end);
            }

            @Override
            public void replaceText(int start, int end, String text) {
                area.replaceText(start, end, text);
            }

            @Override
            public void cut() {
                area.cut();
            }

            @Override
            public void copy() {
                area.copy();
            }

            @Override
            public void paste() {
                area.paste();
            }
        };
    }
}
