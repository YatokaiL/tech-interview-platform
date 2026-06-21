package com.diploma.notification_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSender {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.mail.enabled:true}")
    private boolean enabled;

    /** Returns true if the email was handed to the SMTP server (MailHog). */
    public boolean send(String to, String subject, String body) {
        if (!enabled) {
            log.debug("Mail disabled; skipping email to {}", to);
            return false;
        }
        if (to == null || to.isBlank()) {
            log.debug("No recipient email; skipping");
            return false;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Email sent to {} — {}", to, subject);
            return true;
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
            return false;
        }
    }
}
