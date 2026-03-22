package com.authservice.domain.repository;

import com.authservice.domain.model.AuthMetadata;

import java.util.Optional;
import java.util.UUID;

public interface AuthMetadataRepository {

    AuthMetadata save(AuthMetadata authMetadata);

    Optional<AuthMetadata> findByUserId(UUID userId);
}
