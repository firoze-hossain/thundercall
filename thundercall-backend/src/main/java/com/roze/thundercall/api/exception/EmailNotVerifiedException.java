package com.roze.thundercall.api.exception;

/** Thrown only when login fails specifically because the account's email
 * hasn't been verified yet. Carries the account's REAL email address
 * (not whatever the person typed to log in — that can be a username)
 * so the client can send them straight to the code-entry screen without
 * guessing. */
public class EmailNotVerifiedException extends RuntimeException {
    private final String email;

    public EmailNotVerifiedException(String message, String email) {
        super(message);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}