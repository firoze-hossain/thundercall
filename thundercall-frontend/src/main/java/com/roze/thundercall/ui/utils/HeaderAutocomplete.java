package com.roze.thundercall.ui.utils;

import javafx.collections.FXCollections;
import javafx.geometry.Point2D;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Popup;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Attaches a Postman-style HTTP Header suggestion dropdown to a TextField.
 * Clicking into the field or typing brings up the standard HTTP request headers list.
 */
public final class HeaderAutocomplete {

    public static final List<String> STANDARD_HEADERS = List.of(
            "A-IM",
            "Accept",
            "Accept-CH",
            "Accept-CH-Lifetime",
            "Accept-Charset",
            "Accept-Encoding",
            "Accept-Language",
            "Accept-Patch",
            "Accept-Post",
            "Accept-Ranges",
            "Access-Control-Allow-Credentials",
            "Access-Control-Allow-Headers",
            "Access-Control-Allow-Methods",
            "Access-Control-Allow-Origin",
            "Access-Control-Expose-Headers",
            "Access-Control-Max-Age",
            "Access-Control-Request-Headers",
            "Access-Control-Request-Method",
            "Age",
            "Allow",
            "ALPN",
            "Alt-Svc",
            "Alt-Used",
            "Alternates",
            "Authorization",
            "Cache-Control",
            "Clear-Site-Data",
            "Connection",
            "Content-Disposition",
            "Content-Encoding",
            "Content-Language",
            "Content-Length",
            "Content-Location",
            "Content-MD5",
            "Content-Range",
            "Content-Security-Policy",
            "Content-Security-Policy-Report-Only",
            "Content-Type",
            "Cookie",
            "Cross-Origin-Embedder-Policy",
            "Cross-Origin-Opener-Policy",
            "Cross-Origin-Resource-Policy",
            "Date",
            "Device-Memory",
            "Downlink",
            "DNT",
            "Early-Data",
            "ECT",
            "ETag",
            "Expect",
            "Expect-CT",
            "Expires",
            "Forwarded",
            "From",
            "Host",
            "If-Match",
            "If-Modified-Since",
            "If-None-Match",
            "If-Range",
            "If-Unmodified-Since",
            "Keep-Alive",
            "Large-Allocation",
            "Last-Modified",
            "Link",
            "Location",
            "Max-Forwards",
            "Origin",
            "Origin-Agent-Cluster",
            "Permissions-Policy",
            "Pragma",
            "Proxy-Authenticate",
            "Proxy-Authorization",
            "Range",
            "Referer",
            "Referrer-Policy",
            "Retry-After",
            "RTT",
            "Save-Data",
            "Sec-CH-UA",
            "Sec-CH-UA-Arch",
            "Sec-CH-UA-Bitness",
            "Sec-CH-UA-Full-Version",
            "Sec-CH-UA-Full-Version-List",
            "Sec-CH-UA-Mobile",
            "Sec-CH-UA-Model",
            "Sec-CH-UA-Platform",
            "Sec-CH-UA-Platform-Version",
            "Sec-Fetch-Dest",
            "Sec-Fetch-Mode",
            "Sec-Fetch-Site",
            "Sec-Fetch-User",
            "Sec-GPC",
            "Sec-WebSocket-Accept",
            "Sec-WebSocket-Extensions",
            "Sec-WebSocket-Key",
            "Sec-WebSocket-Protocol",
            "Sec-WebSocket-Version",
            "Server",
            "Server-Timing",
            "Service-Worker-Navigation-Preload",
            "Set-Cookie",
            "SourceMap",
            "Strict-Transport-Security",
            "TE",
            "Timing-Allow-Origin",
            "Tk",
            "Trailer",
            "Transfer-Encoding",
            "Upgrade",
            "Upgrade-Insecure-Requests",
            "User-Agent",
            "Vary",
            "Via",
            "Viewport-Width",
            "Warning",
            "Width",
            "WWW-Authenticate",
            "X-Content-Type-Options",
            "X-DNS-Prefetch-Control",
            "X-Forwarded-For",
            "X-Forwarded-Host",
            "X-Forwarded-Proto",
            "X-Frame-Options",
            "X-Requested-With",
            "X-XSS-Protection"
    );

    private HeaderAutocomplete() {
    }

    /**
     * Attaches HTTP Header suggestion dropdown to the target TextField.
     *
     * @param textField the Key TextField inside a header table cell
     * @param onSelect callback invoked when a suggestion is selected, passing the selected header name
     */
    public static void attach(TextField textField, Consumer<String> onSelect) {
        Popup popup = new Popup();
        popup.setAutoHide(true);
        ListView<String> listView = new ListView<>();
        listView.setPrefWidth(280);
        listView.setMaxHeight(220);
        listView.getStyleClass().add("header-suggestions");

        listView.setItems(FXCollections.observableArrayList(STANDARD_HEADERS));
        popup.getContent().add(listView);

        listView.setFocusTraversable(false);

        Runnable updatePopup = () -> {
            if (!textField.isFocused() || textField.getScene() == null || textField.getScene().getWindow() == null) {
                popup.hide();
                return;
            }
            String query = textField.getText() != null ? textField.getText().trim().toLowerCase() : "";
            List<String> filtered;
            if (query.isEmpty()) {
                filtered = STANDARD_HEADERS;
            } else {
                filtered = STANDARD_HEADERS.stream()
                        .filter(h -> h.toLowerCase().contains(query))
                        .collect(Collectors.toList());
            }

            if (filtered.isEmpty()) {
                popup.hide();
                return;
            }

            listView.getItems().setAll(filtered);
            if (!filtered.isEmpty()) {
                listView.getSelectionModel().select(0);
            }

            double tfWidth = textField.getWidth();
            if (tfWidth > 280) {
                listView.setPrefWidth(tfWidth);
            } else {
                listView.setPrefWidth(280);
            }

            Point2D screen = textField.localToScreen(0, textField.getHeight());
            if (screen != null) {
                if (popup.isShowing()) {
                    popup.setX(screen.getX());
                    popup.setY(screen.getY() + 2);
                } else {
                    popup.show(textField.getScene().getWindow(), screen.getX(), screen.getY() + 2);
                }
            }
        };

        textField.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                updatePopup.run();
            }
        });

        textField.focusedProperty().addListener((obs, oldVal, isFocused) -> {
            if (isFocused) {
                javafx.application.Platform.runLater(updatePopup);
            } else {
                javafx.application.Platform.runLater(() -> {
                    if (!listView.isFocused() && !listView.isHover()) {
                        popup.hide();
                    }
                });
            }
        });

        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (textField.isFocused()) {
                updatePopup.run();
            }
        });

        Consumer<String> selectItem = selected -> {
            if (selected != null) {
                textField.setText(selected);
                textField.positionCaret(selected.length());
                popup.hide();
                if (onSelect != null) {
                    onSelect.accept(selected);
                }
            }
        };

        textField.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (!popup.isShowing()) {
                if (event.getCode() == KeyCode.DOWN) {
                    updatePopup.run();
                    event.consume();
                }
                return;
            }

            if (event.getCode() == KeyCode.DOWN) {
                int next = listView.getSelectionModel().getSelectedIndex() + 1;
                if (next < listView.getItems().size()) {
                    listView.getSelectionModel().select(next);
                    listView.scrollTo(next);
                }
                event.consume();
            } else if (event.getCode() == KeyCode.UP) {
                int prev = listView.getSelectionModel().getSelectedIndex() - 1;
                if (prev >= 0) {
                    listView.getSelectionModel().select(prev);
                    listView.scrollTo(prev);
                }
                event.consume();
            } else if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.TAB) {
                String selected = listView.getSelectionModel().getSelectedItem();
                selectItem.accept(selected);
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                popup.hide();
                event.consume();
            }
        });

        listView.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                String selected = listView.getSelectionModel().getSelectedItem();
                selectItem.accept(selected);
                e.consume();
            }
        });

        listView.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                String selected = listView.getSelectionModel().getSelectedItem();
                selectItem.accept(selected);
                e.consume();
            }
        });
    }
}
