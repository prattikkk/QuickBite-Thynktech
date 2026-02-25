package com.quickbite.sms.service;

/**
 * Abstraction for sending SMS messages.
 */
public interface SmsService {

    /**
     * Send an SMS message.
     *
     * @param to      phone number (E.164 format preferred)
     * @param message text body
     */
    void sendSms(String to, String message);
}
