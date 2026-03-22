package com.authservice.interfaces.rest;

import com.authservice.interfaces.dto.request.LoginRequest;
import com.authservice.interfaces.dto.request.RegisterRequest;
import com.authservice.interfaces.dto.response.CurrentUserResponse;
import com.authservice.interfaces.dto.response.LoginResponse;
import com.authservice.interfaces.dto.response.RegisterResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings("resource")
class AuthStandaloneModeIT {

    @Container
        @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("authdb")
            .withUsername("authuser")
            .withPassword("authpass");

    @Container
        @SuppressWarnings("resource")
    static GenericContainer<?> keycloak = new GenericContainer<>("quay.io/keycloak/keycloak:26.0")
            .withExposedPorts(8080)
            .withEnv("KEYCLOAK_ADMIN", "admin")
            .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin")
            .withCopyFileToContainer(MountableFile.forClasspathResource("realm-test.json"), "/opt/keycloak/data/import/realm-test.json")
            .withCommand("start-dev", "--import-realm");

    static {
        Startables.deepStart(postgres, keycloak).join();
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("keycloak.base-url", () -> "http://" + keycloak.getHost() + ":" + keycloak.getMappedPort(8080));
        registry.add("keycloak.realm", () -> "auth-realm");
        registry.add("keycloak.client-id", () -> "auth-client");
        registry.add("keycloak.client-secret", () -> "auth-client-secret");
        registry.add("keycloak.admin-realm", () -> "master");
        registry.add("keycloak.admin-username", () -> "admin");
        registry.add("keycloak.admin-password", () -> "admin");
        registry.add("keycloak.admin-client-id", () -> "admin-cli");
        registry.add("keycloak.admin-client-secret", () -> "");

        registry.add("auth.mode", () -> "STANDALONE");
        registry.add("auth.default-tenant-api-key", () -> "dev-default-api-key");

        registry.add("resilience4j.circuitbreaker.instances.keycloak.minimum-number-of-calls", () -> "100");
        registry.add("resilience4j.circuitbreaker.instances.keycloak.sliding-window-size", () -> "100");
        registry.add("resilience4j.circuitbreaker.instances.keycloak.failure-rate-threshold", () -> "100");
    }

    @Test
    void fullFlow_ShouldWorkWithoutApiKeyHeader_InStandaloneMode() {
        String email = "standalone-" + UUID.randomUUID() + "@mail.com";
        String externalUserId = "ext-" + UUID.randomUUID();
        String baseUrl = "http://localhost:" + port;

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(email);
        registerRequest.setPassword("StrongPass123!");
        registerRequest.setExternalUserId(externalUserId);

        ResponseEntity<RegisterResponse> registerResponse = restTemplate.exchange(
                baseUrl + "/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(registerRequest),
                RegisterResponse.class
        );

        assertThat(registerResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(Objects.requireNonNull(registerResponse.getBody()).getExternalUserId()).isEqualTo(externalUserId);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(email);
        loginRequest.setPassword("StrongPass123!");

        ResponseEntity<LoginResponse> loginResponse = null;
        HttpStatusCode lastStatus = null;
        for (int attempt = 1; attempt <= 20; attempt++) {
            loginResponse = restTemplate.exchange(
                    baseUrl + "/auth/login",
                    HttpMethod.POST,
                    new HttpEntity<>(loginRequest),
                    LoginResponse.class
            );
            lastStatus = loginResponse.getStatusCode();
            if (loginResponse.getStatusCode().is2xxSuccessful()) {
                break;
            }
            LockSupport.parkNanos(Duration.ofSeconds(2).toNanos());
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
        }

        assertThat(loginResponse).isNotNull();
        assertThat(loginResponse.getStatusCode().is2xxSuccessful())
                .withFailMessage("/auth/login failed, last status: %s", lastStatus)
                .isTrue();
        assertThat(Objects.requireNonNull(loginResponse.getBody()).getAccessToken()).isNotBlank();

        HttpHeaders meHeaders = new HttpHeaders();
        meHeaders.setBearerAuth(loginResponse.getBody().getAccessToken());
        ResponseEntity<CurrentUserResponse> meResponse = restTemplate.exchange(
                baseUrl + "/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(meHeaders),
                CurrentUserResponse.class
        );

        assertThat(meResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(Objects.requireNonNull(meResponse.getBody()).getExternalUserId()).isEqualTo(externalUserId);
    }
}
