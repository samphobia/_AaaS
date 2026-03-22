package com.authservice.infrastructure.persistence.repository;

import com.authservice.domain.model.AuthMetadata;
import com.authservice.domain.repository.AuthMetadataRepository;
import com.authservice.infrastructure.persistence.mapper.AuthMetadataMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AuthMetadataRepositoryAdapter implements AuthMetadataRepository {

    private final SpringDataAuthMetadataRepository springDataAuthMetadataRepository;

    @Override
    public AuthMetadata save(AuthMetadata authMetadata) {
        return AuthMetadataMapper.toDomain(springDataAuthMetadataRepository.save(AuthMetadataMapper.toEntity(authMetadata)));
    }

    @Override
    public Optional<AuthMetadata> findByUserId(UUID userId) {
        return springDataAuthMetadataRepository.findById(userId)
                .map(AuthMetadataMapper::toDomain);
    }
}
