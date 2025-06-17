package com.hrms.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    // Inject values from application.properties for API Info
    @Value("${info.app.name:HRMS API}")
    private String apiTitle;

    @Value("${info.app.version:1.0.0}")
    private String apiVersion;

    @Value("${info.app.description:Comprehensive Human Resource Management System API Documentation}")
    private String apiDescription;

    @Value("${info.app.license.name:Apache 2.0}")
    private String licenseName;

    @Value("${info.app.license.url:https://www.apache.org/licenses/LICENSE-2.0}")
    private String licenseUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        Info apiInfo = new Info()
            .title(apiTitle)
            .version(apiVersion)
            .description(apiDescription)
            .license(new License().name(licenseName).url(licenseUrl));

        return new OpenAPI()
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT Bearer token for authentication. Enter token value with 'Bearer ' prefix (e.g., Bearer eyJhbGciOi...).")
                )
            )
            .info(apiInfo);
    }
}
