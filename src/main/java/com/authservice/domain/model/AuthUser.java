package com.authservice.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class AuthUser {
    UUID id;
    String keycloakUserId;
    String externalUserId;
    Set<Role> roles;
    UUID tenantId;
    boolean active;
    Instant createdAt;
}
