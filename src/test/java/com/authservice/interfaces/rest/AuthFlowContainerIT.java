package com.authservice.interfaces.rest;

import com.authservice.interfaces.dto.request.LoginRequest;
import com.authservice.interfaces.dto.request.RefreshTokenRequest;
import com.authservice.interfaces.dto.request.RegisterRequest;
import com.authservice.interfaces.dto.request.ServiceTokenRequest;
import com.authservice.interfaces.dto.response.CurrentUserResponse;
import com.authservice.interfaces.dto.response.LoginResponse;
import com.authservice.interfaces.dto.response.RegisterResponse;
import com.authservice.interfaces.dto.response.ServicePrincipalResponse;
import com.authservice.interfaces.dto.response.ServiceTokenResponse;
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

import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthFlowContainerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("authdb")
            .withUsername("authuser")
            .withPassword("authpass");

    @Container
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

                registry.add("resilience4j.circuitbreaker.instances.keycloak.minimum-number-of-calls", () -> "100");
                registry.add("resilience4j.circuitbreaker.instances.keycloak.sliding-window-size", () -> "100");
                registry.add("resilience4j.circuitbreaker.instances.keycloak.failure-rate-threshold", () -> "100");
    }

    @Test
    void fullAuthFlow_ShouldRegisterLoginRefreshAndResolveCurrentUser() {
        String email = "it-" + UUID.randomUUID() + "@mail.com";
        String externalUserId = "ext-" + UUID.randomUUID();
        String baseUrl = "http://localhost:" + port;

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(email);
        registerRequest.setPassword("StrongPass123!");
        registerRequest.setExternalUserId(externalUserId);

        ResponseEntity<RegisterResponse> registerResponse = restTemplate.exchange(
                baseUrl + "/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(registerRequest, apiHeaders()),
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
                                        new HttpEntity<>(loginRequest, apiHeaders()),
                                        LoginResponse.class
                        );
                        lastStatus = loginResponse.getStatusCode();
                        if (loginResponse.getStatusCode().is2xxSuccessful()) {
                                break;
                        }
                        try {
                                Thread.sleep(2000);
                        } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                        }
                }

                assertThat(loginResponse).isNotNull();
        assertThat(loginResponse.getStatusCode().is2xxSuccessful())
                .withFailMessage("/auth/login failed, last status: %s", lastStatus)
                .isTrue();
        assertThat(Objects.requireNonNull(loginResponse.getBody()).getAccessToken()).isNotBlank();
        assertThat(loginResponse.getBody().getRefreshToken()).isNotBlank();

        HttpHeaders meHeaders = apiHeaders();
        meHeaders.setBearerAuth(loginResponse.getBody().getAccessToken());
        ResponseEntity<CurrentUserResponse> meResponse = restTemplate.exchange(
                baseUrl + "/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(meHeaders),
                CurrentUserResponse.class
        );

        assertThat(meResponse.getStatusCode().is2xxSuccessful())
                .withFailMessage("/auth/me failed status=%s", meResponse.getStatusCode())
                .isTrue();
        assertThat(Objects.requireNonNull(meResponse.getBody()).getExternalUserId()).isEqualTo(externalUserId);

        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest();
        refreshTokenRequest.setRefreshToken(loginResponse.getBody().getRefreshToken());
        ResponseEntity<LoginResponse> refreshResponse = restTemplate.exchange(
                baseUrl + "/auth/refresh",
                HttpMethod.POST,
                new HttpEntity<>(refreshTokenRequest, apiHeaders()),
                LoginResponse.class
        );

        assertThat(refreshResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(Objects.requireNonNull(refreshResponse.getBody()).getAccessToken()).isNotBlank();

        ServiceTokenRequest serviceTokenRequest = new ServiceTokenRequest();
        serviceTokenRequest.setClientId("auth-client");
        serviceTokenRequest.setClientSecret("auth-client-secret");
        serviceTokenRequest.setScope("service.read");

        ResponseEntity<ServiceTokenResponse> serviceTokenResponse = restTemplate.exchange(
                baseUrl + "/auth/service-token",
                HttpMethod.POST,
                new HttpEntity<>(serviceTokenRequest, apiHeaders()),
                ServiceTokenResponse.class
        );

        assertThat(serviceTokenResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(Objects.requireNonNull(serviceTokenResponse.getBody()).getAccessToken()).isNotBlank();

        HttpHeaders serviceHeaders = apiHeaders();
        serviceHeaders.setBearerAuth(serviceTokenResponse.getBody().getAccessToken());
        ResponseEntity<ServicePrincipalResponse> serviceMeResponse = restTemplate.exchange(
                baseUrl + "/auth/service/me",
                HttpMethod.GET,
                new HttpEntity<>(serviceHeaders),
                ServicePrincipalResponse.class
        );

        assertThat(serviceMeResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(Objects.requireNonNull(serviceMeResponse.getBody()).getPrincipal()).isEqualTo("auth-client");
        assertThat(serviceMeResponse.getBody().getScopes()).contains("service.read");
    }

    private HttpHeaders apiHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-KEY", "dev-default-api-key");
        return headers;
    }
}
