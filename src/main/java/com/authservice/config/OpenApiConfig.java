package com.authservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

        private static final String BEARER_AUTH = "bearerAuth";
        private static final String API_KEY_AUTH = "apiKeyAuth";
        private static final String TENANT_HEADER = "X-API-KEY";

        @Bean
        public OpenAPI authServiceOpenApi(AuthProperties authProperties) {
                boolean multiTenantMode = authProperties.getMode() == AuthMode.MULTI_TENANT;

                Components components = new Components()
                                .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT"));

                if (multiTenantMode) {
                        components.addSecuritySchemes(API_KEY_AUTH, new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name(TENANT_HEADER));
                }

                OpenAPI openAPI = new OpenAPI()
                                .info(new Info()
                                                .title("Authentication and Authorization Service")
                                                .version("v1")
                                                .description("Multi-tenant auth orchestration layer backed by Keycloak")
                                                .contact(new Contact().name("Platform Security Team")))
                                .components(components)
                                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));

                if (multiTenantMode) {
                        openAPI.addSecurityItem(new SecurityRequirement().addList(API_KEY_AUTH));
                }

                return openAPI;
        }

        @Bean
        public OpenApiCustomizer standaloneOpenApiCustomizer(AuthProperties authProperties) {
                return openApi -> {
                        if (authProperties.getMode() != AuthMode.STANDALONE) {
                                return;
                        }

                        if (openApi.getSecurity() != null) {
                                openApi.setSecurity(openApi.getSecurity().stream()
                                                .filter(securityRequirement -> !securityRequirement.containsKey(API_KEY_AUTH))
                                                .toList());
                        }

                        if (openApi.getComponents() != null && openApi.getComponents().getSecuritySchemes() != null) {
                                openApi.getComponents().getSecuritySchemes().remove(API_KEY_AUTH);
                        }

                        if (openApi.getPaths() == null) {
                                return;
                        }

                        openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
                                if (operation.getSecurity() != null) {
                                        operation.setSecurity(operation.getSecurity().stream()
                                                        .filter(securityRequirement -> !securityRequirement.containsKey(API_KEY_AUTH))
                                                        .toList());
                                }

                                if (operation.getParameters() != null) {
                                        operation.setParameters(operation.getParameters().stream()
                                                        .filter(parameter -> !("header".equalsIgnoreCase(parameter.getIn()) && TENANT_HEADER.equalsIgnoreCase(parameter.getName())))
                                                        .toList());
                                }

                                if (operation.getResponses() != null) {
                                        operation.getResponses().values().forEach(apiResponse -> {
                                                String description = apiResponse.getDescription();
                                                if (description == null || description.isBlank()) {
                                                        return;
                                                }

                                                String updatedDescription = description
                                                                .replace("Missing or invalid X-API-KEY", "Unauthorized")
                                                                .replace(" or missing API key", "");
                                                apiResponse.setDescription(updatedDescription);
                                        });
                                }
                        }));
                };
        }
}
