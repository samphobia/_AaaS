package com.authservice.infrastructure.persistence.mapper;

import com.authservice.domain.model.CustomAttributeDefinition;
import com.authservice.infrastructure.persistence.entity.CustomAttributeDefinitionEntity;
import com.authservice.infrastructure.persistence.entity.TenantEntity;

public final class CustomAttributeDefinitionMapper {

    private CustomAttributeDefinitionMapper() {
    }

    public static CustomAttributeDefinition toDomain(CustomAttributeDefinitionEntity entity) {
        return CustomAttributeDefinition.builder()
                .id(entity.getId())
                .tenantId(entity.getTenant().getId())
                .name(entity.getName())
                .type(entity.getType())
                .required(entity.isRequired())
                .build();
    }

    public static CustomAttributeDefinitionEntity toEntity(CustomAttributeDefinition model) {
        return CustomAttributeDefinitionEntity.builder()
                .id(model.getId())
                .tenant(TenantEntity.builder().id(model.getTenantId()).build())
                .name(model.getName())
                .type(model.getType())
                .required(model.isRequired())
                .build();
    }
}
