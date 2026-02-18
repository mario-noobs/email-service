package com.mario.email;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.email")
public class EmailProperties {

    private String fromAddress = "noreply@example.com";
    private String fromName = "System";
    private String templatePath = "email-templates";
    private boolean enabled = true;
}
