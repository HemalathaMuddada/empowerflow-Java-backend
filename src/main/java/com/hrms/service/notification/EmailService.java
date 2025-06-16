package com.hrms.service.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context; // Added for Thymeleaf context
import org.thymeleaf.spring6.SpringTemplateEngine; // Added for Thymeleaf engine
import org.springframework.beans.factory.annotation.Qualifier; // Added for Qualifier

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false) // Make optional if mail config might not be present
    private JavaMailSender mailSender;

    @Autowired(required = false) // Keep it optional as before
    @Qualifier("emailTemplateEngine") // Specify which template engine
    private SpringTemplateEngine emailTemplateEngine;

    // Use spring.mail.from if set, otherwise fallback to spring.mail.username, then a hardcoded default.
    @Value("${spring.mail.from:${spring.mail.username:noreply@hrms.example.com}}")
    private String fromAddress;


    @Async
    public void sendSimpleMail(String to, String subject, String text) {
        if (mailSender == null) {
            logger.warn("MailSender not configured. Email to {} with subject '{}' not sent.", to, subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            logger.info("Simple email sent successfully to {}", to);
        } catch (MailException e) {
            logger.error("Error sending simple email to {}: {}", to, e.getMessage(), e);
        }
    }

    @Async
    public void sendHtmlMail(String to, String subject, String htmlContent) {
        if (mailSender == null) {
            logger.warn("MailSender not configured. HTML Email to {} with subject '{}' not sent.", to, subject);
            return;
        }
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8"); // true for multipart if needed

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true indicates HTML content

            mailSender.send(mimeMessage);
            logger.info("HTML email sent successfully to {}", to);
        } catch (MessagingException | MailException e) { // Catch specific MessagingException for MimeMessageHelper
            logger.error("Error sending HTML email to {}: {}", to, e.getMessage(), e);
        }
    }

    @Async
    public void sendHtmlMailFromTemplate(String to, String subject, String templateName, Context context) {
        if (mailSender == null || emailTemplateEngine == null) {
            logger.warn("MailSender or EmailTemplateEngine not configured. Email (template: {}) to {} with subject '{}' not sent.", templateName, to, subject);
            return;
        }
        try {
            String htmlBody = emailTemplateEngine.process(templateName, context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true indicates HTML

            mailSender.send(mimeMessage);
            logger.info("HTML email from template '{}' sent successfully to {}", templateName, to);

        } catch (MessagingException | MailException e) {
            logger.error("Error sending HTML email from template {} to {}: {}", templateName, to, e.getMessage(), e);
        } catch (Exception e) { // Catch Thymeleaf processing errors
            logger.error("Error processing Thymeleaf template {} for email to {}: {}", templateName, to, e.getMessage(), e);
        }
    }
}
