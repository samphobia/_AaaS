package com.authservice.infrastructure.keycloak;

import com.authservice.application.exception.UnauthorizedException;
import com.authservice.application.service.IdentityProviderClient;
import com.authservice.application.service.model.TokenPair;
import com.authservice.application.service.model.TokenValidationResult;
import com.authservice.config.KeycloakProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakClient implements IdentityProviderClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String CUSTOM_ATTRIBUTES_KEY = "custom_attributes";

    private final RestClient restClient;
    private final KeycloakProperties keycloakProperties;

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
    public String createUser(String email, String password) {
        String adminToken = requestAdminToken();

        Map<String, Object> payload = Map.of(
                "username", email,
                "email", email,
            "firstName", "Auth",
            "lastName", "User",
            "enabled", true,
            "emailVerified", true,
            "requiredActions", List.of()
        );

        try {
            var response = restClient.post()
                    .uri(keycloakProperties.getBaseUrl() + "/admin/realms/" + keycloakProperties.getRealm() + "/users")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            URI location = response.getHeaders().getLocation();
            if (location == null) {
                throw new IllegalStateException("Keycloak did not return created user location");
            }
            String path = location.getPath();
            String userId = path.substring(path.lastIndexOf('/') + 1);
            setUserPassword(adminToken, userId, password);
            finalizeUserSetup(adminToken, userId, email);
            assignRealmRolesWithAdminToken(adminToken, userId, Set.of("USER"));
            return userId;
        } catch (HttpStatusCodeException ex) {
            log.error("Failed to create user in Keycloak. status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new IllegalStateException("Failed to create user in identity provider");
        }
    }

    private void setUserPassword(String adminToken, String userId, String password) {
        Map<String, Object> passwordPayload = Map.of(
                "type", "password",
                "value", password,
                "temporary", false
        );

        restClient.put()
                .uri(keycloakProperties.getBaseUrl() + "/admin/realms/" + keycloakProperties.getRealm() + "/users/" + userId + "/reset-password")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(passwordPayload)
                .retrieve()
                .toBodilessEntity();
    }

            private void finalizeUserSetup(String adminToken, String userId, String email) {
            Map<String, Object> updatePayload = Map.of(
                "id", userId,
                "username", email,
                "email", email,
                "firstName", "Auth",
                "lastName", "User",
                "enabled", true,
                "emailVerified", true,
                "requiredActions", List.of()
            );

            restClient.put()
                .uri(keycloakProperties.getBaseUrl() + "/admin/realms/" + keycloakProperties.getRealm() + "/users/" + userId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updatePayload)
                .retrieve()
                .toBodilessEntity();
            }

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
    public TokenPair getToken(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", keycloakProperties.getClientId());
        form.add("scope", "openid");
        form.add("username", username);
        form.add("password", password);
        return requestToken(form);
    }

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
    public TokenPair refreshToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", keycloakProperties.getClientId());
        form.add("refresh_token", refreshToken);
        return requestToken(form);
    }

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
    public TokenPair getServiceToken(String clientId, String clientSecret, String scope) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        if (scope != null && !scope.isBlank()) {
            form.add("scope", scope);
        }
        return requestToken(form, clientId, clientSecret);
    }

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
    public TokenValidationResult validateToken(String token) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("token", token);
        form.add("client_id", keycloakProperties.getClientId());

        Map<String, Object> response;
        try {
            var request = restClient.post()
                    .uri(keycloakProperties.getBaseUrl() + "/realms/" + keycloakProperties.getRealm() + "/protocol/openid-connect/token/introspect")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED);
            applyClientAuth(request);
            response = request.body(form)
                    .retrieve()
                    .body(Map.class);
        } catch (HttpStatusCodeException ex) {
            log.warn("Token introspection failed status={}", ex.getStatusCode());
            throw new UnauthorizedException("Invalid token");
        }

        if (response == null || !Boolean.TRUE.equals(response.get("active"))) {
            throw new UnauthorizedException("Token is not active");
        }

        Map<String, Object> jwtClaims = decodeJwtClaims(token);

        String subject = (String) response.get("sub");
        String username = (String) response.get("username");
        if ((subject == null || subject.isBlank()) && username != null && !username.isBlank() && !isMachineToken(response)) {
            subject = resolveSubjectFromUserInfo(token);
        }

        return TokenValidationResult.builder()
                .active(true)
                .subject(subject)
                .username(username)
                .clientId((String) response.get("client_id"))
                .machineToken(isMachineToken(response))
                .roles(merge(extractRoles(response), extractRoles(jwtClaims)))
                .scopes(merge(extractScopes(response), extractScopes(jwtClaims)))
                .build();
    }

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
    public void assignRealmRoles(String userId, Set<String> roles) {
        assignRealmRolesWithAdminToken(requestAdminToken(), userId, roles);
    }

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
    public void syncUserAttributes(String userId, Map<String, Object> attributes) {
        String adminToken = requestAdminToken();
        Map<String, Object> userRepresentation = getUserRepresentation(adminToken, userId);

        Map<String, List<String>> existingAttributes = extractExistingAttributes(userRepresentation);
        existingAttributes.put(CUSTOM_ATTRIBUTES_KEY, List.of(serializeCustomAttributes(attributes)));

        userRepresentation.put("attributes", existingAttributes);
        restClient.put()
                .uri(keycloakProperties.getBaseUrl() + "/admin/realms/" + keycloakProperties.getRealm() + "/users/" + userId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(userRepresentation)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
    public void initiatePasswordReset(String email) {
        String adminToken = requestAdminToken();
        String userId = findUserIdByEmail(adminToken, email);
        if (userId == null) {
            log.info("Password reset requested for non-existing user email={}", email);
            return;
        }

        restClient.put()
                .uri(keycloakProperties.getBaseUrl() + "/admin/realms/" + keycloakProperties.getRealm() + "/users/" + userId + "/execute-actions-email")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of("UPDATE_PASSWORD"))
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
    public void changePassword(String accessToken, String currentPassword, String newPassword) {
        Map<String, String> payload = Map.of(
                "currentPassword", currentPassword,
                "newPassword", newPassword,
                "confirmation", newPassword
        );

        try {
            restClient.post()
                    .uri(keycloakProperties.getBaseUrl() + "/realms/" + keycloakProperties.getRealm() + "/account/credentials/password")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED || ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
                log.warn("Password change rejected status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
                throw new UnauthorizedException("Invalid current password or token");
            }
            log.error("Password change failed status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new IllegalStateException("Failed to communicate with identity provider");
        }
    }

    private TokenPair requestToken(MultiValueMap<String, String> form) {
        return requestToken(form, keycloakProperties.getClientId(), keycloakProperties.getClientSecret());
    }

    private TokenPair requestToken(MultiValueMap<String, String> form, String clientId, String clientSecret) {
        try {
            var request = restClient.post()
                    .uri(keycloakProperties.getBaseUrl() + "/realms/" + keycloakProperties.getRealm() + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED);
            applyClientAuth(request, clientId, clientSecret);
            Map<String, Object> response = request.body(form)
                .retrieve()
                .body(Map.class);

            if (response == null) {
                throw new UnauthorizedException("Token endpoint returned no payload");
            }

            return TokenPair.builder()
                    .accessToken((String) response.get("access_token"))
                    .refreshToken((String) response.get("refresh_token"))
                    .expiresIn(((Number) response.getOrDefault("expires_in", 0)).longValue())
                    .build();
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED || ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
                log.warn("Keycloak token request rejected status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
                throw new UnauthorizedException("Invalid credentials or token");
            }
            log.error("Keycloak token request failed status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new IllegalStateException("Failed to communicate with identity provider");
        }
    }

    private void applyClientAuth(RestClient.RequestBodySpec request) {
        applyClientAuth(request, keycloakProperties.getClientId(), keycloakProperties.getClientSecret());
    }

    private void applyClientAuth(RestClient.RequestBodySpec request, String clientId, String secret) {
        if (secret != null && !secret.isBlank()) {
            request.headers(headers -> headers.setBasicAuth(clientId, secret));
        }
    }

    private String requestAdminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", keycloakProperties.getAdminClientId());
        if (keycloakProperties.getAdminClientSecret() != null && !keycloakProperties.getAdminClientSecret().isBlank()) {
            form.add("client_secret", keycloakProperties.getAdminClientSecret());
        }
        form.add("username", keycloakProperties.getAdminUsername());
        form.add("password", keycloakProperties.getAdminPassword());

        Map<String, Object> response = restClient.post()
            .uri(keycloakProperties.getBaseUrl() + "/realms/" + keycloakProperties.getAdminRealm() + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("access_token") == null) {
            throw new IllegalStateException("Failed to obtain admin token");
        }
        return (String) response.get("access_token");
    }

    private void assignRealmRolesWithAdminToken(String adminToken, String userId, Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return;
        }

        List<Map<String, Object>> roleRepresentations = new ArrayList<>();
        for (String roleName : roles) {
            if (roleName == null || roleName.isBlank()) {
                continue;
            }
            Map<String, Object> roleRepresentation = restClient.get()
                    .uri(keycloakProperties.getBaseUrl() + "/admin/realms/" + keycloakProperties.getRealm() + "/roles/" + roleName)
                    .header("Authorization", "Bearer " + adminToken)
                    .retrieve()
                    .body(Map.class);
            if (roleRepresentation != null) {
                roleRepresentations.add(roleRepresentation);
            }
        }

        if (roleRepresentations.isEmpty()) {
            return;
        }

        restClient.post()
                .uri(keycloakProperties.getBaseUrl() + "/admin/realms/" + keycloakProperties.getRealm() + "/users/" + userId + "/role-mappings/realm")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(roleRepresentations)
                .retrieve()
                .toBodilessEntity();
    }

    @SuppressWarnings("unchecked")
    private String findUserIdByEmail(String adminToken, String email) {
        String uri = UriComponentsBuilder
                .fromHttpUrl(keycloakProperties.getBaseUrl() + "/admin/realms/" + keycloakProperties.getRealm() + "/users")
                .queryParam("email", email)
                .queryParam("exact", true)
                .build()
                .toUriString();

        List<Map<String, Object>> users = restClient.get()
                .uri(uri)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .body(List.class);

        if (users == null || users.isEmpty()) {
            return null;
        }

        Object id = users.get(0).get("id");
        if (!(id instanceof String userId) || userId.isBlank()) {
            return null;
        }
        return userId;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getUserRepresentation(String adminToken, String userId) {
        Map<String, Object> response = restClient.get()
                .uri(keycloakProperties.getBaseUrl() + "/admin/realms/" + keycloakProperties.getRealm() + "/users/" + userId)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .body(Map.class);
        if (response == null) {
            throw new IllegalStateException("Failed to load user from identity provider");
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<String>> extractExistingAttributes(Map<String, Object> userRepresentation) {
        Object attributes = userRepresentation.get("attributes");
        if (!(attributes instanceof Map<?, ?> raw)) {
            return new java.util.HashMap<>();
        }

        Map<String, List<String>> normalized = new java.util.HashMap<>();
        raw.forEach((key, value) -> {
            if (key == null) {
                return;
            }
            String normalizedKey = String.valueOf(key);
            if (value instanceof List<?> list) {
                normalized.put(normalizedKey, list.stream().filter(Objects::nonNull).map(String::valueOf).toList());
            } else if (value != null) {
                normalized.put(normalizedKey, List.of(String.valueOf(value)));
            }
        });
        return normalized;
    }

    private String serializeCustomAttributes(Map<String, Object> attributes) {
        Map<String, Object> safeAttributes = attributes == null ? Map.of() : attributes;
        try {
            return OBJECT_MAPPER.writeValueAsString(safeAttributes);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize custom attributes", ex);
        }
    }

    private Map<String, Object> decodeJwtClaims(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return Map.of();
            }
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            return OBJECT_MAPPER.readValue(payload, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private Set<String> merge(Set<String> first, Set<String> second) {
        if (first.isEmpty()) {
            return second;
        }
        if (second.isEmpty()) {
            return first;
        }
        Set<String> merged = new HashSet<>(first);
        merged.addAll(second);
        return Set.copyOf(merged);
    }

    @SuppressWarnings("unchecked")
    private Set<String> extractRoles(Map<String, Object> introspectionResponse) {
        Object realmAccess = introspectionResponse.get("realm_access");
        if (!(realmAccess instanceof Map<?, ?> realmAccessMap)) {
            return Set.of();
        }
        Object roles = ((Map<String, Object>) realmAccessMap).get("roles");
        if (!(roles instanceof List<?> rolesList)) {
            return Set.of();
        }

        Set<String> result = new HashSet<>();
        for (Object role : rolesList) {
            if (role instanceof String roleValue && !roleValue.isBlank()) {
                result.add(roleValue);
            }
        }
        return Set.copyOf(result);
    }

    private Set<String> extractScopes(Map<String, Object> introspectionResponse) {
        Object rawScope = introspectionResponse.get("scope");
        if (!(rawScope instanceof String scopeString) || scopeString.isBlank()) {
            return Set.of();
        }

        Set<String> scopes = new HashSet<>();
        for (String scope : scopeString.split("\\s+")) {
            if (!scope.isBlank()) {
                scopes.add(scope);
            }
        }
        return Set.copyOf(scopes);
    }

    private boolean isMachineToken(Map<String, Object> introspectionResponse) {
        Object subject = introspectionResponse.get("sub");
        if (subject instanceof String subjectValue && subjectValue.startsWith("service-account-")) {
            return true;
        }

        Object username = introspectionResponse.get("username");
        return username instanceof String usernameValue && usernameValue.startsWith("service-account-");
    }

    @SuppressWarnings("unchecked")
    private String resolveSubjectFromUserInfo(String accessToken) {
        try {
            Map<String, Object> userInfoResponse = restClient.get()
                    .uri(keycloakProperties.getBaseUrl() + "/realms/" + keycloakProperties.getRealm() + "/protocol/openid-connect/userinfo")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            if (userInfoResponse == null) {
                return null;
            }
            Object sub = userInfoResponse.get("sub");
            if (!(sub instanceof String subject) || subject.isBlank()) {
                return null;
            }
            return subject;
        } catch (HttpStatusCodeException ex) {
            log.warn("Userinfo request failed status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return null;
        }
    }
}
