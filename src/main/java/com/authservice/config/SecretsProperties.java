package com.authservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "auth.secrets")
public class SecretsProperties {

    private String provider = "env";
    private final Kms kms = new Kms();

    @Data
    public static class Kms {
        private boolean enabled;
        private String region;
        private String keyId;
        private String encryptedClientSecret;
    }
}
