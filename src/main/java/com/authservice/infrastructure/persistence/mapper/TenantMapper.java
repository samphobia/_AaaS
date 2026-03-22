package com.authservice.infrastructure.persistence.mapper;

import com.authservice.domain.model.Tenant;
import com.authservice.infrastructure.persistence.entity.TenantEntity;

public final class TenantMapper {

    private TenantMapper() {
    }

    public static Tenant toDomain(TenantEntity entity) {
        return Tenant.builder()
                .id(entity.getId())
                .name(entity.getName())
                .apiKey(entity.getApiKey())
                .build();
    }
}
