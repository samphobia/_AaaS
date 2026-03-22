package com.authservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    private AuthMode mode = AuthMode.MULTI_TENANT;
    private String defaultTenantApiKey = "dev-default-api-key";
}