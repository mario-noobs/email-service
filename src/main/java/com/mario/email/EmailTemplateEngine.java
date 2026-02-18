package com.mario.email;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

public class EmailTemplateEngine {

    private static final Logger log = LoggerFactory.getLogger(EmailTemplateEngine.class);

    private final Configuration freemarkerConfig;

    public EmailTemplateEngine(String templatePath) {
        this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_32);
        this.freemarkerConfig.setClassLoaderForTemplateLoading(
                getClass().getClassLoader(), templatePath);
        this.freemarkerConfig.setDefaultEncoding("UTF-8");
        this.freemarkerConfig.setLogTemplateExceptions(false);
    }

    public String render(String templateName, Map<String, Object> model) {
        try {
            Template template = freemarkerConfig.getTemplate(templateName + ".ftl");
            StringWriter writer = new StringWriter();
            template.process(model, writer);
            return writer.toString();
        } catch (IOException | TemplateException e) {
            log.error("Failed to render email template: {}", templateName, e);
            throw new EmailException("Failed to render email template: " + templateName, e);
        }
    }
}
