package com.authservice.infrastructure.persistence.mapper;

import com.authservice.domain.model.AuthMetadata;
import com.authservice.infrastructure.persistence.entity.AuthMetadataEntity;

public final class AuthMetadataMapper {

    private AuthMetadataMapper() {
    }

    public static AuthMetadata toDomain(AuthMetadataEntity entity) {
        return AuthMetadata.builder()
                .userId(entity.getUserId())
                .lastLoginAt(entity.getLastLoginAt())
                .lastLoginIp(entity.getLastLoginIp())
                .device(entity.getDevice())
                .build();
    }

    public static AuthMetadataEntity toEntity(AuthMetadata model) {
        return AuthMetadataEntity.builder()
                .userId(model.getUserId())
                .lastLoginAt(model.getLastLoginAt())
                .lastLoginIp(model.getLastLoginIp())
                .device(model.getDevice())
                .build();
    }
}
