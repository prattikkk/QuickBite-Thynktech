package com.quickbite.email.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.util.Map;

/**
 * Production email service using SendGrid API.
 * Active when email.provider=sendgrid.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "email.provider", havingValue = "sendgrid")
public class SendGridEmailService implements EmailService {

    private final SendGrid sendGrid;
    private final TemplateEngine templateEngine;
    private final String fromEmail;
    private final String fromName;

    public SendGridEmailService(
            @Value("${email.sendgrid.api-key}") String apiKey,
            @Value("${email.from-address:noreply@quickbite.app}") String fromEmail,
            @Value("${email.from-name:QuickBite}") String fromName,
            TemplateEngine templateEngine) {
        this.sendGrid = new SendGrid(apiKey);
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.templateEngine = templateEngine;
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        Email from = new Email(fromEmail, fromName);
        Email toEmail = new Email(to);
        Content content = new Content("text/plain", body);
        Mail mail = new Mail(from, subject, toEmail, content);
        doSend(mail, to, subject);
    }

    @Override
    public void sendTemplatedEmail(String to, String subject, String template, Map<String, Object> variables) {
        Context ctx = new Context();
        ctx.setVariables(variables);
        String htmlContent = templateEngine.process("email/" + template, ctx);

        Email from = new Email(fromEmail, fromName);
        Email toEmail = new Email(to);
        Content content = new Content("text/html", htmlContent);
        Mail mail = new Mail(from, subject, toEmail, content);
        doSend(mail, to, subject);
    }

    private void doSend(Mail mail, String to, String subject) {
        try {
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("Email sent via SendGrid to={} subject='{}' status={}", to, subject, response.getStatusCode());
            } else {
                log.error("SendGrid error: status={} body={}", response.getStatusCode(), response.getBody());
            }
        } catch (IOException e) {
            log.error("Failed to send email via SendGrid to={}: {}", to, e.getMessage(), e);
        }
    }
}
