package com.authservice.infrastructure.persistence.repository;

import com.authservice.domain.model.AuthUser;
import com.authservice.domain.repository.AuthUserRepository;
import com.authservice.infrastructure.persistence.mapper.AuthUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AuthUserRepositoryAdapter implements AuthUserRepository {

    private final SpringDataAuthUserRepository springDataAuthUserRepository;

    @Override
    public AuthUser save(AuthUser authUser) {
        return AuthUserMapper.toDomain(springDataAuthUserRepository.save(AuthUserMapper.toEntity(authUser)));
    }

    @Override
    public Optional<AuthUser> findByKeycloakUserIdAndTenantId(String keycloakUserId, UUID tenantId) {
        return springDataAuthUserRepository.findByKeycloakUserIdAndTenant_Id(keycloakUserId, tenantId)
                .map(AuthUserMapper::toDomain);
    }

    @Override
    public Optional<AuthUser> findByExternalUserIdAndTenantId(String externalUserId, UUID tenantId) {
        return springDataAuthUserRepository.findByExternalUserIdAndTenant_Id(externalUserId, tenantId)
                .map(AuthUserMapper::toDomain);
    }
}
