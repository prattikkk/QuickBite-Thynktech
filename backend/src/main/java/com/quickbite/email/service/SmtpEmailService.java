package com.quickbite.email.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Production email service using SMTP (works with Gmail, Outlook, Mailgun, etc.).
 * Active when email.provider=smtp.
 *
 * Configure via environment variables:
 *   EMAIL_PROVIDER=smtp
 *   SMTP_HOST=smtp.gmail.com
 *   SMTP_PORT=587
 *   SMTP_USERNAME=you@gmail.com
 *   SMTP_PASSWORD=your-app-password   (Gmail: generate at myaccount.google.com/apppasswords)
 *   EMAIL_FROM=you@gmail.com
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "email.provider", havingValue = "smtp")
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String fromEmail;
    private final String fromName;

    public SmtpEmailService(
            JavaMailSender mailSender,
            TemplateEngine templateEngine,
            @Value("${email.from-address:noreply@quickbite.app}") String fromEmail,
            @Value("${email.from-name:QuickBite}") String fromName) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
            log.info("Email sent via SMTP to={} subject='{}'", to, subject);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send email via SMTP to={} subject='{}': {}", to, subject, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending email via SMTP to={}: {}", to, e.getMessage(), e);
        }
    }

    @Override
    public void sendTemplatedEmail(String to, String subject, String template, Map<String, Object> variables) {
        try {
            Context ctx = new Context();
            ctx.setVariables(variables);
            String htmlContent = templateEngine.process("email/" + template, ctx);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML
            mailSender.send(message);
            log.info("Templated email sent via SMTP to={} template={}", to, template);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send templated email via SMTP to={} template={}: {}", to, template, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending templated email via SMTP to={}: {}", to, e.getMessage(), e);
        }
    }
}
