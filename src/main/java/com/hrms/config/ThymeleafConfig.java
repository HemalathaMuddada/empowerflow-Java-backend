package com.hrms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

// Not including ResourceBundleMessageSource for i18n in this step as it's optional.
// It can be added later if messages in emails need localization.

@Configuration
public class ThymeleafConfig {

    @Bean(name = "emailTemplateEngine")
    public SpringTemplateEngine emailTemplateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(emailTemplateResolver());
        // For example, if you want to use SpringEL expressions:
        // templateEngine.setEnableSpringELCompiler(true);
        return templateEngine;
    }

    private ITemplateResolver emailTemplateResolver() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        // Specifies that templates are in src/main/resources/templates/mail/
        templateResolver.setPrefix("templates/mail/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        // Set to true in production for better performance
        templateResolver.setCacheable(false);
        templateResolver.setOrder(1); // In case other template resolvers are configured
        return templateResolver;
    }
}
