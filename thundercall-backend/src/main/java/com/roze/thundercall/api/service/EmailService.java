package com.roze.thundercall.api.service;

public interface EmailService {
    /** Sends a plain-text email using whatever SMTP settings are
     * currently stored. Throws if mail isn't configured/enabled, or if
     * the send itself fails — callers decide whether that should block
     * the calling operation or just be logged. */
    void send(String toAddress, String subject, String body);
}
