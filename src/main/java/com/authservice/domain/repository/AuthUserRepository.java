package com.authservice.domain.repository;

import com.authservice.domain.model.AuthUser;

import java.util.Optional;
import java.util.UUID;

public interface AuthUserRepository {

    AuthUser save(AuthUser authUser);

    Optional<AuthUser> findByKeycloakUserIdAndTenantId(String keycloakUserId, UUID tenantId);

    Optional<AuthUser> findByExternalUserIdAndTenantId(String externalUserId, UUID tenantId);
}
