package com.authservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
@Slf4j
public class KmsSecretBootstrap {

    private final KeycloakProperties keycloakProperties;
    private final SecretsProperties secretsProperties;

    @jakarta.annotation.PostConstruct
    public void bootstrap() {
        if (!secretsProperties.getKms().isEnabled()) {
            return;
        }
        if (!keycloakProperties.getClientSecret().isBlank()) {
            return;
        }

        String encrypted = secretsProperties.getKms().getEncryptedClientSecret();
        if (encrypted == null || encrypted.isBlank()) {
            throw new IllegalStateException("KMS is enabled but encrypted client secret is missing");
        }

        try (KmsClient kmsClient = KmsClient.builder()
                .region(Region.of(secretsProperties.getKms().getRegion()))
                .build()) {

            DecryptRequest request = DecryptRequest.builder()
                    .ciphertextBlob(software.amazon.awssdk.core.SdkBytes.fromByteArray(Base64.getDecoder().decode(encrypted)))
                    .keyId(secretsProperties.getKms().getKeyId())
                    .build();

            ByteBuffer plaintextBuffer = kmsClient.decrypt(request).plaintext().asByteBuffer();
            byte[] plaintext = new byte[plaintextBuffer.remaining()];
            plaintextBuffer.get(plaintext);
            keycloakProperties.setClientSecret(new String(plaintext, StandardCharsets.UTF_8));
            log.info("Loaded Keycloak client secret from AWS KMS");
        }
    }
}
