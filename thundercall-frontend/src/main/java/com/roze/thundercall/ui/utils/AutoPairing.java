package com.roze.thundercall.ui.utils;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.fxmisc.richtext.CodeArea;

import java.util.Map;

/**
 * Postman-style auto-pairing for quotes and brackets in a RichTextFX
 * CodeArea, matching how virtually every code editor (Postman's body
 * editor included) behaves:
 * <ul>
 *   <li>Typing an opening delimiter — {@code "}, {@code '}, {@code (},
 *       {@code [} — inserts its matching close and leaves the caret
 *       between them, so {@code "} becomes {@code "|"} (caret at {@code |}).</li>
 *   <li>Typing a closing delimiter — or the SAME quote character again —
 *       right in front of its own already-there partner "types through"
 *       instead of inserting a duplicate, so finishing a string you just
 *       auto-opened doesn't leave a stray extra quote.</li>
 *   <li>Backspace on an empty pair (caret sitting between two delimiters
 *       with nothing typed inside yet) removes both sides in one go.</li>
 * </ul>
 * <p>
 * {@code {}/} is intentionally NOT handled for the opening side here —
 * {@link VariableAutocomplete}, attached to the same editors, already
 * auto-closes "{" (needed for "{{variable}}" support); duplicating that
 * logic here would insert a second closing brace. The closing "}" and
 * Backspace-pair-delete cases are still handled here for consistency.
 */
public final class AutoPairing {

    private static final Map<Character, Character> OPEN_TO_CLOSE = Map.of(
            '(', ')',
            '[', ']'
    );

    /** Closing brackets that, when typed and already sitting immediately
     * after the caret, should be "typed through" rather than duplicated.
     * (Quotes get the same treatment, but are handled separately above
     * since "open" and "close" are the same character for them.) */
    private static final String SKIP_OVER_CLOSERS = ")]}";

    private AutoPairing() {
    }

    public static void attach(CodeArea area) {
        boolean[] selfEdit = {false};

        area.plainTextChanges().subscribe(change -> {
            if (selfEdit[0]) {
                return;
            }
            String inserted = change.getInserted();
            // Only react to a genuine single-character keystroke, never a
            // paste or a programmatic multi-character replace.
            if (inserted.length() != 1 || !change.getRemoved().isEmpty()) {
                return;
            }
            char typed = inserted.charAt(0);
            int caret = area.getCaretPosition();
            String text = area.getText();
            boolean sittingBeforeSameChar = caret < text.length() && text.charAt(caret) == typed;

            if (typed == '"' || typed == '\'') {
                selfEdit[0] = true;
                if (sittingBeforeSameChar) {
                    // Closing an auto-opened pair: drop the just-typed
                    // quote and step over the one already there.
                    area.deleteText(caret - 1, caret);
                    area.moveTo(caret);
                } else {
                    area.insertText(caret, String.valueOf(typed));
                    area.moveTo(caret);
                }
                selfEdit[0] = false;
                return;
            }

            Character close = OPEN_TO_CLOSE.get(typed);
            if (close != null) {
                boolean nextIsClose = caret < text.length() && text.charAt(caret) == close;
                if (!nextIsClose) {
                    selfEdit[0] = true;
                    area.insertText(caret, String.valueOf(close));
                    area.moveTo(caret);
                    selfEdit[0] = false;
                }
                return;
            }

            if (SKIP_OVER_CLOSERS.indexOf(typed) >= 0 && sittingBeforeSameChar) {
                selfEdit[0] = true;
                area.deleteText(caret - 1, caret);
                area.moveTo(caret);
                selfEdit[0] = false;
            }
        });

        // Backspace over an empty pair removes both delimiters together
        // instead of leaving the closing half dangling.
        area.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() != KeyCode.BACK_SPACE) {
                return;
            }
            int caret = area.getCaretPosition();
            String text = area.getText();
            if (caret <= 0 || caret >= text.length()) {
                return;
            }
            char before = text.charAt(caret - 1);
            char after = text.charAt(caret);
            boolean isEmptyPair = (before == '"' && after == '"')
                    || (before == '\'' && after == '\'')
                    || (before == '(' && after == ')')
                    || (before == '[' && after == ']')
                    || (before == '{' && after == '}');
            if (isEmptyPair) {
                area.deleteText(caret - 1, caret + 1);
                event.consume();
            }
        });
    }
}
