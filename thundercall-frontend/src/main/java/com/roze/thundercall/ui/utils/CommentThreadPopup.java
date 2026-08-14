package com.roze.thundercall.ui.utils;

import com.roze.thundercall.ui.models.CommentResponse;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Postman's inline body-comment popups: the "Ask questions or provide
 * feedback..." composer shown right after selecting text and choosing
 * Comment, and the thread viewer (breadcrumb + messages + reply box +
 * Edit/Delete on your own messages + resolve toggle) shown when reopening
 * an existing highlighted comment.
 */
public final class CommentThreadPopup {

    public interface NewThreadCallback {
        void onSubmit(String message);
    }

    public interface ThreadCallback {
        void onReply(String message);

        void onEdit(Long commentId, String newMessage);

        void onDelete(Long commentId);

        void onToggleResolve(boolean resolve);
    }

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MMM d, h:mm a");

    private CommentThreadPopup() {
    }

    /** The new-comment composer (Postman's images 1-2). */
    public static void showComposer(Node anchor, NewThreadCallback callback) {
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        TextArea textArea = new TextArea();
        textArea.setPromptText("Ask questions or provide feedback. Use @mention to notify people.");
        textArea.setWrapText(true);
        textArea.setPrefRowCount(3);
        textArea.getStyleClass().add("comment-input");

        Button commentButton = new Button("Comment");
        commentButton.getStyleClass().add("popup-submit-button");
        commentButton.setDisable(true);
        textArea.textProperty().addListener((o, ov, nv) -> commentButton.setDisable(nv.trim().isEmpty()));

        commentButton.setOnAction(e -> {
            String text = textArea.getText().trim();
            if (text.isEmpty()) {
                return;
            }
            callback.onSubmit(text);
            popup.hide();
        });

        HBox buttonRow = new HBox(commentButton);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);

        VBox box = new VBox(8, textArea, buttonRow);
        box.getStyleClass().addAll("root", "comment-popup");
        box.setPadding(new Insets(10));
        box.setPrefWidth(320);
        applyStylesheets(anchor, box);

        popup.getContent().add(box);
        showNear(popup, anchor);
        textArea.requestFocus();
    }

    /** The thread viewer — breadcrumb, message list, reply box (Postman's
     * images 3-4). {@code currentUserId} decides which messages get an
     * Edit/Delete menu. */
    public static void showThread(Node anchor, CommentResponse thread, String breadcrumb,
                                   Long currentUserId, ThreadCallback callback) {
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        VBox box = new VBox();
        box.getStyleClass().addAll("root", "comment-popup");
        box.setPrefWidth(360);

        Label breadcrumbLabel = new Label(breadcrumb);
        breadcrumbLabel.getStyleClass().add("comment-breadcrumb");
        VBox header = new VBox(breadcrumbLabel);
        header.setPadding(new Insets(10, 12, 6, 12));
        box.getChildren().addAll(header, new Separator());

        VBox messagesBox = new VBox();
        messagesBox.setPadding(new Insets(4, 0, 4, 0));

        List<CommentResponse> allMessages = new ArrayList<>();
        allMessages.add(thread);
        allMessages.addAll(thread.getRepliesOrEmpty());
        for (CommentResponse msg : allMessages) {
            messagesBox.getChildren().add(buildMessageRow(msg, msg.getId().equals(thread.getId()), thread, currentUserId, callback));
        }
        box.getChildren().addAll(messagesBox, new Separator());

        TextField replyField = new TextField();
        replyField.setPromptText("Add a reply");
        replyField.getStyleClass().add("comment-reply-field");
        HBox.setHgrow(replyField, Priority.ALWAYS);
        replyField.setOnAction(e -> {
            String text = replyField.getText().trim();
            if (!text.isEmpty()) {
                callback.onReply(text);
                replyField.clear();
            }
        });
        HBox replyRow = new HBox(8, replyField);
        replyRow.setPadding(new Insets(8, 12, 10, 12));
        replyRow.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(replyRow);

        applyStylesheets(anchor, box);
        popup.getContent().add(box);
        showNear(popup, anchor);
    }

    private static Node buildMessageRow(CommentResponse msg, boolean isRoot, CommentResponse rootThread,
                                         Long currentUserId, ThreadCallback callback) {
        Label authorLabel = new Label(msg.getAuthorName());
        authorLabel.getStyleClass().add("comment-author");
        Label timeLabel = new Label(formatTime(msg.getCreatedAt()));
        timeLabel.getStyleClass().add("comment-timestamp");

        HBox nameRow = new HBox(8, authorLabel, timeLabel);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameRow, Priority.ALWAYS);

        HBox topRow = new HBox(nameRow);
        topRow.setAlignment(Pos.CENTER_LEFT);

        if (isRoot) {
            boolean resolved = Boolean.TRUE.equals(rootThread.getResolved());
            Button resolveButton = new Button(resolved ? "\u21BA" : "\u2713");
            resolveButton.getStyleClass().add("comment-icon-button");
            resolveButton.setTooltip(new Tooltip(resolved ? "Reopen" : "Resolve"));
            resolveButton.setOnAction(e -> callback.onToggleResolve(!resolved));
            topRow.getChildren().add(resolveButton);
        }

        Label messageLabel = new Label(msg.getMessage());
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("comment-message");

        VBox row = new VBox(4, topRow, messageLabel);
        row.setPadding(new Insets(8, 12, 8, 12));

        boolean isOwn = currentUserId != null && currentUserId.equals(msg.getAuthorId());
        if (isOwn) {
            TextField editField = new TextField(msg.getMessage());
            editField.setVisible(false);
            editField.setManaged(false);
            editField.setOnAction(e -> {
                String updated = editField.getText().trim();
                if (!updated.isEmpty()) {
                    callback.onEdit(msg.getId(), updated);
                }
            });
            row.getChildren().add(editField);

            Button menuButton = new Button("\u22EF");
            menuButton.getStyleClass().add("comment-icon-button");
            ContextMenu menu = new ContextMenu();
            MenuItem editItem = new MenuItem("Edit");
            editItem.setOnAction(e -> {
                messageLabel.setVisible(false);
                messageLabel.setManaged(false);
                editField.setVisible(true);
                editField.setManaged(true);
                editField.requestFocus();
                editField.selectAll();
            });
            MenuItem deleteItem = new MenuItem("Delete");
            deleteItem.getStyleClass().add("comment-delete-item");
            deleteItem.setOnAction(e -> callback.onDelete(msg.getId()));
            menu.getItems().addAll(editItem, deleteItem);
            menuButton.setOnAction(e -> menu.show(menuButton, Side.BOTTOM, 0, 0));
            topRow.getChildren().add(menuButton);
        }

        return row;
    }

    private static String formatTime(LocalDateTime dt) {
        return dt != null ? dt.format(TIME_FORMAT) : "";
    }

    private static void applyStylesheets(Node anchor, Parent content) {
        if (anchor.getScene() != null) {
            content.getStylesheets().addAll(anchor.getScene().getRoot().getStylesheets());
        }
    }

    private static void showNear(Popup popup, Node anchor) {
        Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        double x = bounds != null ? bounds.getMinX() + 24 : 150;
        double y = bounds != null ? bounds.getMinY() + 24 : 150;
        popup.show(anchor, x, y);
        PopupDismissal.closeOnOutsideClick(popup, anchor);
    }
}
