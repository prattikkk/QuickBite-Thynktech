package com.quickbite.email.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Development/test email service that logs emails to console instead of sending.
 * Active when email.provider=console (default).
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "email.provider", havingValue = "console", matchIfMissing = true)
public class ConsoleEmailService implements EmailService {

    @Override
    public void sendEmail(String to, String subject, String body) {
        log.info("========== CONSOLE EMAIL ==========");
        log.info("To:      {}", to);
        log.info("Subject: {}", subject);
        log.info("Body:    {}", body);
        log.info("===================================");
    }

    @Override
    public void sendTemplatedEmail(String to, String subject, String template, Map<String, Object> variables) {
        log.info("========== CONSOLE TEMPLATED EMAIL ==========");
        log.info("To:       {}", to);
        log.info("Subject:  {}", subject);
        log.info("Template: {}", template);
        log.info("Variables: {}", variables);
        log.info("==============================================");
    }
}
