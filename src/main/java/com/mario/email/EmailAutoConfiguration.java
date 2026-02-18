package com.mario.email;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;

@AutoConfiguration
@ConditionalOnClass(JavaMailSender.class)
@EnableConfigurationProperties(EmailProperties.class)
public class EmailAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public EmailTemplateEngine emailTemplateEngine(EmailProperties properties) {
        return new EmailTemplateEngine(properties.getTemplatePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public EmailService emailService(JavaMailSender mailSender,
                                     EmailTemplateEngine templateEngine,
                                     EmailProperties properties) {
        return new EmailService(mailSender, templateEngine, properties);
    }
}
