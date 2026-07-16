package com.roze.thundercall.api.service.impl;

import com.roze.thundercall.api.entity.MailSettings;
import com.roze.thundercall.api.repository.MailSettingsRepository;
import com.roze.thundercall.api.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Properties;

/**
 * Sends mail using whatever is CURRENTLY in the mail_settings table,
 * rebuilt fresh on every call rather than a single Spring-managed
 * JavaMailSender bean — this is what makes SMTP settings "dynamic": the
 * project owner can change them from the Settings screen and the very
 * next email uses the new values, no server restart required.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {
    private final MailSettingsRepository mailSettingsRepository;

    @Override
    public void send(String toAddress, String subject, String body) {
        MailSettings settings = mailSettingsRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Mail isn't set up yet — go to Settings and configure the SMTP server first."));

        if (!settings.isEnabled()) {
            throw new IllegalStateException(
                    "Mail sending is currently turned off in Settings — enable it to send this email.");
        }

        JavaMailSenderImpl mailSender = buildSender(settings);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(toAddress);
            helper.setSubject(subject);
            helper.setText(body, false);
            String fromName = settings.getFromName();
            if (fromName != null && !fromName.isBlank()) {
                helper.setFrom(settings.getFromAddress(), fromName);
            } else {
                helper.setFrom(settings.getFromAddress());
            }
            mailSender.send(message);
            log.info("Sent email to {} (subject: {})", toAddress, subject);
        } catch (MailException | java.io.UnsupportedEncodingException | jakarta.mail.MessagingException e) {
            log.error("Failed to send email to {}: {}", toAddress, e.getMessage());
            throw new IllegalStateException("Couldn't send the email — check the SMTP settings and try again. ("
                    + e.getMessage() + ")", e);
        }
    }

    private JavaMailSenderImpl buildSender(MailSettings settings) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(settings.getSmtpHost());
        mailSender.setPort(settings.getSmtpPort());
        if (settings.getSmtpUsername() != null && !settings.getSmtpUsername().isBlank()) {
            mailSender.setUsername(settings.getSmtpUsername());
        }
        if (settings.getSmtpPassword() != null && !settings.getSmtpPassword().isBlank()) {
            mailSender.setPassword(settings.getSmtpPassword());
        }

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", settings.getSmtpUsername() != null && !settings.getSmtpUsername().isBlank());
        props.put("mail.smtp.starttls.enable", settings.isUseTls());
        props.put("mail.smtp.ssl.enable", settings.isUseSsl());
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        return mailSender;
    }
}
