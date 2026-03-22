package com.authservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

    private String baseUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
    private String adminRealm;
    private String adminUsername;
    private String adminPassword;
    private String adminClientId;
    private String adminClientSecret;
}
