package com.mario.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final EmailTemplateEngine templateEngine;
    private final EmailProperties properties;

    public EmailService(JavaMailSender mailSender, EmailTemplateEngine templateEngine,
                        EmailProperties properties) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.properties = properties;
    }

    public void send(EmailRequest request) {
        if (!properties.isEnabled()) {
            log.debug("Email sending disabled, skipping: {}", request.getSubject());
            return;
        }

        String html = templateEngine.render(request.getTemplateName(), request.getTemplateModel());

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String fromAddress = request.getFromAddress() != null
                    ? request.getFromAddress() : properties.getFromAddress();
            helper.setFrom(fromAddress, properties.getFromName());
            helper.setTo(request.getTo());
            helper.setSubject(request.getSubject());
            helper.setText(html, true);

            if (request.getCc() != null && !request.getCc().isEmpty()) {
                helper.setCc(request.getCc().toArray(new String[0]));
            }
            if (request.getBcc() != null && !request.getBcc().isEmpty()) {
                helper.setBcc(request.getBcc().toArray(new String[0]));
            }

            if (request.getHeaders() != null) {
                for (var entry : request.getHeaders().entrySet()) {
                    message.addHeader(entry.getKey(), entry.getValue());
                }
            }

            mailSender.send(message);
            log.info("Email sent successfully: to={}, subject={}", request.getTo(), request.getSubject());

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send email: to={}, subject={}", request.getTo(), request.getSubject(), e);
            throw new EmailException("Failed to send email to " + request.getTo(), e);
        }
    }
}
