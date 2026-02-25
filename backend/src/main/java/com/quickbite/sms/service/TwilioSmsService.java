package com.quickbite.sms.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

/**
 * Production SMS service using Twilio REST API.
 * Active when sms.provider=twilio.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "sms.provider", havingValue = "twilio")
public class TwilioSmsService implements SmsService {

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final RestTemplate restTemplate = new RestTemplate();

    public TwilioSmsService(
            @Value("${sms.twilio.account-sid}") String accountSid,
            @Value("${sms.twilio.auth-token}") String authToken,
            @Value("${sms.twilio.from-number}") String fromNumber) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
    }

    @Override
    public void sendSms(String to, String message) {
        try {
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            String auth = Base64.getEncoder().encodeToString((accountSid + ":" + authToken).getBytes());
            headers.set("Authorization", "Basic " + auth);

            String body = "From=" + fromNumber + "&To=" + to + "&Body=" + message;

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("SMS sent via Twilio to={}", to);
            } else {
                log.error("Twilio SMS error: status={}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to send SMS via Twilio to={}: {}", to, e.getMessage(), e);
        }
    }
}
