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
    private TextField passwordTextField;
    @FXML
    private Button togglePasswordButton;
    @FXML
    private javafx.scene.layout.VBox emailBox;
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
    private Label switchPromptLabel;
    @FXML
    private ProgressIndicator loadingSpinner;
    @FXML
    private Label authTitle;
    @FXML
    private Label authSubtitle;
    @FXML
    private CheckBox rememberMeCheck;

    private Boolean isLoginMode = true;
    private boolean passwordVisible = false;

    @FXML
    public void initialize() {
        if (passwordTextField != null) {
            passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());
        }
        if (togglePasswordButton != null) {
            togglePasswordButton.setOnAction(e -> togglePasswordVisibility());
        }

        // Interactive keyboard navigation: Enter advances field or submits
        usernameField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                if (!isLoginMode && emailField != null && emailField.isManaged()) {
                    emailField.requestFocus();
                } else if (passwordField.isVisible()) {
                    passwordField.requestFocus();
                } else if (passwordTextField != null && passwordTextField.isVisible()) {
                    passwordTextField.requestFocus();
                }
            }
        });

        if (emailField != null) {
            emailField.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    if (passwordField.isVisible()) {
                        passwordField.requestFocus();
                    } else if (passwordTextField != null && passwordTextField.isVisible()) {
                        passwordTextField.requestFocus();
                    }
                }
            });
        }

        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                handleAuthAction();
            }
        });

        if (passwordTextField != null) {
            passwordTextField.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    handleAuthAction();
                }
            });
        }

        updateViewMode();
        // Remember me: pre-fill the most recent saved account
        if (rememberMeCheck != null && CredentialStore.hasSaved()) {
            usernameField.setText(CredentialStore.savedUsername());
            passwordField.setText(CredentialStore.savedPassword());
            rememberMeCheck.setSelected(true);
        }
        setupCredentialSuggestions();
    }

    /**
     * Browser-style suggestions: clicking the username field shows the saved
     * accounts; picking one fills BOTH username and password.
     */
    private void setupCredentialSuggestions() {
        usernameField.setOnMouseClicked(e -> showAccountSuggestions());
    }

    private void showAccountSuggestions() {
        List<String> accounts = CredentialStore.savedUsernames();
        if (accounts.isEmpty()) {
            return;
        }
        ContextMenu suggestions = new ContextMenu();
        for (String account : accounts) {
            MenuItem item = new MenuItem("\uD83D\uDC64  " + account);
            item.setOnAction(ev -> {
                usernameField.setText(account);
                passwordField.setText(CredentialStore.passwordFor(account));
                if (rememberMeCheck != null) {
                    rememberMeCheck.setSelected(true);
                }
            });
            suggestions.getItems().add(item);
        }
        SeparatorMenuItem sep = new SeparatorMenuItem();
        MenuItem forget = new MenuItem("Forget saved accounts");
        forget.setOnAction(ev -> CredentialStore.clear());
        suggestions.getItems().addAll(sep, forget);
        suggestions.show(usernameField, javafx.geometry.Side.BOTTOM, 0, 0);
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
                    if (e.getMessage() != null && e.getMessage().contains("verify your email")) {
                        // The account exists but hasn't been verified yet.
                        // The server sends the account's REAL email back in
                        // "details" — what was typed here could be a
                        // username, not an email, so we use that instead
                        // of guessing (falls back to what was typed only
                        // if the server didn't include it, e.g. an older
                        // backend build).
                        setLoading(false);
                        String realEmail = extractDetailEmail(e.getMessage());
                        Main.showVerifyEmailView(realEmail != null ? realEmail : usernameOrEmail);
                    } else {
                        AlertUtils.showError("Login failed: " + e.getMessage());
                        setLoading(false);
                    }
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
                com.roze.thundercall.ui.models.RegisterResponse response = ApiClient.register(username, email, password);
                Platform.runLater(() -> {
                    AlertUtils.showSuccess(response.getMessage());
                    Main.showVerifyEmailView(response.getEmail());
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    AlertUtils.showError("Registration failed: " + e.getMessage());
                    setLoading(false);
                });
            }
        }).start();
    }

    /** Pulls the first value out of the raw error's "details":["..."]
     * array, which is where the server puts the account's real email for
     * the "please verify your email" case. Returns null if it's not
     * there (older backend, or a different kind of error). */
    private String extractDetailEmail(String rawErrorMessage) {
        if (rawErrorMessage == null) {
            return null;
        }
        int idx = rawErrorMessage.indexOf("\"details\":[\"");
        if (idx < 0) {
            return null;
        }
        int start = idx + "\"details\":[\"".length();
        int end = rawErrorMessage.indexOf('"', start);
        return end > start ? rawErrorMessage.substring(start, end) : null;
    }

    @FXML
    private void switchViewMode() {
        isLoginMode = !isLoginMode;
        updateViewMode();
    }

    public void setRegisterMode(boolean registerMode) {
        this.isLoginMode = !registerMode;
        updateViewMode();
    }

    private void togglePasswordVisibility() {
        if (passwordTextField == null || togglePasswordButton == null) {
            return;
        }
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            passwordTextField.setVisible(true);
            passwordTextField.setManaged(true);
            togglePasswordButton.setText("🔒");
            passwordTextField.requestFocus();
            passwordTextField.positionCaret(passwordTextField.getText().length());
        } else {
            passwordTextField.setVisible(false);
            passwordTextField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            togglePasswordButton.setText("👁");
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getText().length());
        }
    }

    private void updateViewMode() {
        if (isLoginMode) {
            if (emailBox != null) {
                emailBox.setVisible(false);
                emailBox.setManaged(false);
            }
            if (emailLabel != null) {
                emailLabel.setVisible(false);
                emailLabel.setManaged(false);
            }
            if (emailField != null) {
                emailField.setVisible(false);
                emailField.setManaged(false);
            }

            loginButton.setVisible(true);
            loginButton.setManaged(true);

            registerButton.setVisible(false);
            registerButton.setManaged(false);

            switchToRegisterLink.setVisible(true);
            switchToRegisterLink.setManaged(true);

            switchToLoginLink.setVisible(false);
            switchToLoginLink.setManaged(false);

            if (switchPromptLabel != null) {
                switchPromptLabel.setText("New to Thundercall?");
            }

            usernameField.setPromptText("Enter username or email");

            if (authTitle != null) {
                authTitle.setText("Welcome back");
            }
            if (authSubtitle != null) {
                authSubtitle.setText("Sign in to continue to your workspace");
            }
        } else {
            if (emailBox != null) {
                emailBox.setVisible(true);
                emailBox.setManaged(true);
            }
            if (emailLabel != null) {
                emailLabel.setVisible(true);
                emailLabel.setManaged(true);
            }
            if (emailField != null) {
                emailField.setVisible(true);
                emailField.setManaged(true);
            }

            loginButton.setVisible(false);
            loginButton.setManaged(false);
            registerButton.setVisible(true);
            registerButton.setManaged(true);

            switchToRegisterLink.setVisible(false);
            switchToRegisterLink.setManaged(false);
            switchToLoginLink.setVisible(true);
            switchToLoginLink.setManaged(true);

            if (switchPromptLabel != null) {
                switchPromptLabel.setText("Already have an account?");
            }

            usernameField.setPromptText("Enter username");

            if (authTitle != null) {
                authTitle.setText("Create your account");
            }
            if (authSubtitle != null) {
                authSubtitle.setText("Start building and testing your APIs in seconds");
            }
        }
    }

    private void setLoading(boolean loading) {
        loadingSpinner.setVisible(loading);
        loginButton.setDisable(loading);
        registerButton.setDisable(loading);
        browserLoginButton.setDisable(loading);
        if (togglePasswordButton != null) {
            togglePasswordButton.setDisable(loading);
        }
    }

    @FXML
    private void handleBrowserLogin() {
        DeepLinkService.openBrowserForAuth();
    }
}