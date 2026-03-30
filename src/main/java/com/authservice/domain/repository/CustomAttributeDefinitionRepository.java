package com.authservice.domain.repository;

import com.authservice.domain.model.CustomAttributeDefinition;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomAttributeDefinitionRepository {

    CustomAttributeDefinition save(CustomAttributeDefinition definition);

    Optional<CustomAttributeDefinition> findByTenantIdAndName(UUID tenantId, String name);

    List<CustomAttributeDefinition> findByTenantId(UUID tenantId);

    List<CustomAttributeDefinition> findRequiredByTenantId(UUID tenantId);

    List<CustomAttributeDefinition> findByIdsAndTenantId(Collection<UUID> ids, UUID tenantId);
}
