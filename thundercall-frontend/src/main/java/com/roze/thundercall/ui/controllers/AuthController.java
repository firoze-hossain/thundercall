package com.roze.thundercall.ui.controllers;

import com.roze.thundercall.ui.Main;
import com.roze.thundercall.ui.models.AuthResponse;
import com.roze.thundercall.ui.services.ApiClient;
import com.roze.thundercall.ui.services.DeepLinkService;
import com.roze.thundercall.ui.services.TokenManager;
import com.roze.thundercall.ui.utils.AlertUtils;
import com.roze.thundercall.ui.utils.CredentialStore;
import com.roze.thundercall.ui.utils.Validator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.util.List;

/**
 * FIXES over the previous version:
 *  1. Login mode previously called emailLabel.setManaged(false) twice instead
 *     of emailField.setManaged(false), so the invisible email field still
 *     occupied layout space. Fixed.
 *  2. New authTitle / authSubtitle labels (null-safe, so this controller also
 *     works with the old FXML) switch between "Welcome back" and
 *     "Create your account" with the view mode.
 */
public class AuthController {
    @FXML
    private TextField usernameField;
    @FXML
    private TextField emailField;
    @FXML
    private Label emailLabel;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;
    @FXML
    private Button registerButton;
    @FXML
    private Button browserLoginButton;
    @FXML
    private Hyperlink switchToRegisterLink;
    @FXML
    private Hyperlink switchToLoginLink;
    @FXML
    private ProgressIndicator loadingSpinner;
    @FXML
    private Label authTitle;
    @FXML
    private Label authSubtitle;
    @FXML
    private CheckBox rememberMeCheck;

    private Boolean isLoginMode = true;

    @FXML
    public void initialize() {
        updateViewMode();
        // Remember me: pre-fill saved credentials so one click signs in
        if (rememberMeCheck != null && CredentialStore.hasSaved()) {
            usernameField.setText(CredentialStore.savedUsername());
            passwordField.setText(CredentialStore.savedPassword());
            rememberMeCheck.setSelected(true);
        }
    }

    @FXML
    private void handleAuthAction() {
        if (isLoginMode) {
            handleLogin();
        } else {
            handleRegister();
        }
    }

    @FXML
    private void handleLogin() {
        String usernameOrEmail = usernameField.getText();
        String password = passwordField.getText();
        if (usernameOrEmail.isEmpty() || password.isEmpty()) {
            AlertUtils.showError("Please fill the all fields");
            return;
        }
        setLoading(true);

        new Thread(() -> {
            try {
                AuthResponse response = ApiClient.login(usernameOrEmail, password);
                Platform.runLater(() -> {
                    if (rememberMeCheck != null && rememberMeCheck.isSelected()) {
                        CredentialStore.save(usernameOrEmail, password);
                    } else {
                        CredentialStore.clear();
                    }
                    TokenManager.storeTokens(response);
                    Main.showMainView();
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    AlertUtils.showError("Login failed: " + e.getMessage());
                    setLoading(false);
                });
            }
        }).start();
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        List<String> errors = Validator.validateRegistration(username, email, password);
        if (!errors.isEmpty()) {
            AlertUtils.showError(String.join("\n", errors));
            return;
        }
        setLoading(true);
        new Thread(() -> {
            try {
                AuthResponse response = ApiClient.register(username, email, password);
                Platform.runLater(() -> {
                    TokenManager.storeTokens(response);
                    AlertUtils.showSuccess("Registration successful");
                    Main.showMainView();
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    AlertUtils.showError("Registration failed: " + e.getMessage());
                    setLoading(false);
                });
            }
        }).start();
    }

    @FXML
    private void switchViewMode() {
        isLoginMode = !isLoginMode;
        updateViewMode();
    }

    private void updateViewMode() {
        if (isLoginMode) {
            emailLabel.setVisible(false);
            emailLabel.setManaged(false);
            emailField.setVisible(false);
            emailField.setManaged(false);   // FIX: was emailLabel.setManaged(false)

            loginButton.setVisible(true);
            loginButton.setManaged(true);

            registerButton.setVisible(false);
            registerButton.setManaged(false);

            switchToRegisterLink.setVisible(true);
            switchToRegisterLink.setManaged(true);

            switchToLoginLink.setVisible(false);
            switchToLoginLink.setManaged(false);

            usernameField.setPromptText("Enter username or email");

            if (authTitle != null) {
                authTitle.setText("Welcome back");
            }
            if (authSubtitle != null) {
                authSubtitle.setText("Sign in to continue to your workspace");
            }
        } else {
            emailLabel.setVisible(true);
            emailLabel.setManaged(true);
            emailField.setVisible(true);
            emailField.setManaged(true);

            loginButton.setVisible(false);
            loginButton.setManaged(false);
            registerButton.setVisible(true);
            registerButton.setManaged(true);

            switchToRegisterLink.setVisible(false);
            switchToRegisterLink.setManaged(false);
            switchToLoginLink.setVisible(true);
            switchToLoginLink.setManaged(true);

            usernameField.setPromptText("Enter username");

            if (authTitle != null) {
                authTitle.setText("Create your account");
            }
            if (authSubtitle != null) {
                authSubtitle.setText("A ready-to-use workspace is waiting for you");
            }
        }
    }

    private void setLoading(boolean loading) {
        loadingSpinner.setVisible(loading);
        loginButton.setDisable(loading);
        registerButton.setDisable(loading);
        browserLoginButton.setDisable(loading);
    }

    @FXML
    private void handleBrowserLogin() {
        DeepLinkService.openBrowserForAuth();
    }
}