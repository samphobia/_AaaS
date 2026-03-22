package com.authservice.infrastructure.persistence.repository;

import com.authservice.infrastructure.persistence.entity.AuthUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataAuthUserRepository extends JpaRepository<AuthUserEntity, UUID> {

    Optional<AuthUserEntity> findByKeycloakUserIdAndTenant_Id(String keycloakUserId, UUID tenantId);

    Optional<AuthUserEntity> findByExternalUserIdAndTenant_Id(String externalUserId, UUID tenantId);
}
