package com.quickbite.sms.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Dev/test SMS service — logs messages to console.
 * Active when sms.provider=console (default).
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "sms.provider", havingValue = "console", matchIfMissing = true)
public class ConsoleSmsService implements SmsService {

    @Override
    public void sendSms(String to, String message) {
        log.info("========== CONSOLE SMS ==========");
        log.info("To:      {}", to);
        log.info("Message: {}", message);
        log.info("=================================");
    }
}
