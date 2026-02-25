package com.quickbite.email.service;

import java.util.Map;

/**
 * Abstraction for sending emails.
 * Implementations: ConsoleEmailService (dev), SmtpEmailService, SendGridEmailService.
 */
public interface EmailService {

    /**
     * Send a plain-text email.
     */
    void sendEmail(String to, String subject, String body);

    /**
     * Send a templated HTML email.
     *
     * @param to         recipient
     * @param subject    subject line
     * @param template   Thymeleaf template name (e.g. "password-reset")
     * @param variables  template variables
     */
    void sendTemplatedEmail(String to, String subject, String template, Map<String, Object> variables);
}
