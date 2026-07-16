package com.roze.thundercall.ui.controllers;

import com.roze.thundercall.ui.Main;
import com.roze.thundercall.ui.models.AuthResponse;
import com.roze.thundercall.ui.services.ApiClient;
import com.roze.thundercall.ui.services.TokenManager;
import com.roze.thundercall.ui.utils.AlertUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

import java.io.IOException;

public class VerifyEmailController {
    @FXML
    private Label subtitleLabel;
    @FXML
    private TextField codeField;
    @FXML
    private Button verifyButton;
    @FXML
    private ProgressIndicator loadingSpinner;
    @FXML
    private Label statusLabel;

    private String email;

    /** Called right after Main loads this view, so the screen knows which
     * address to verify/resend for without asking the person to retype it. */
    public void setEmail(String email) {
        this.email = email;
        if (subtitleLabel != null) {
            subtitleLabel.setText("Enter the 6-digit code we sent to " + email);
        }
    }

    @FXML
    private void handleVerify() {
        String code = codeField.getText() == null ? "" : codeField.getText().trim();
        if (code.isEmpty()) {
            statusLabel.setText("Enter the code from your email first.");
            return;
        }
        setLoading(true);
        new Thread(() -> {
            try {
                AuthResponse response = ApiClient.verifyEmail(email, code);
                Platform.runLater(() -> {
                    TokenManager.storeTokens(response);
                    AlertUtils.showSuccess("Email verified — welcome to Thundercall!");
                    Main.showMainView();
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    statusLabel.setText(friendlyMessage(e.getMessage()));
                    setLoading(false);
                });
            }
        }).start();
    }

    @FXML
    private void handleResend() {
        setLoading(true);
        statusLabel.setText("Sending a new code...");
        new Thread(() -> {
            try {
                ApiClient.resendVerificationCode(email);
                Platform.runLater(() -> {
                    statusLabel.setText("A new code is on its way to " + email + ".");
                    setLoading(false);
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    statusLabel.setText(friendlyMessage(e.getMessage()));
                    setLoading(false);
                });
            }
        }).start();
    }

    @FXML
    private void handleBackToLogin() {
        Main.showLoginView();
    }

    private void setLoading(boolean loading) {
        loadingSpinner.setVisible(loading);
        verifyButton.setDisable(loading);
    }

    /** Strips the "HTTP 4xx: {...}" wrapper down to just the server's own
     * message where possible, so the person sees "That code doesn't
     * match" instead of a raw JSON blob. */
    private String friendlyMessage(String raw) {
        if (raw == null) {
            return "Something went wrong — try again.";
        }
        int messageIdx = raw.indexOf("\"message\":\"");
        if (messageIdx >= 0) {
            int start = messageIdx + "\"message\":\"".length();
            int end = raw.indexOf('"', start);
            if (end > start) {
                return raw.substring(start, end);
            }
        }
        return raw;
    }
}
