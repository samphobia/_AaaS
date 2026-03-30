package com.authservice.infrastructure.persistence.repository;

import com.authservice.domain.model.CustomAttributeDefinition;
import com.authservice.domain.repository.CustomAttributeDefinitionRepository;
import com.authservice.infrastructure.persistence.mapper.CustomAttributeDefinitionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CustomAttributeDefinitionRepositoryAdapter implements CustomAttributeDefinitionRepository {

    private final SpringDataCustomAttributeDefinitionRepository springDataCustomAttributeDefinitionRepository;

    @Override
    public CustomAttributeDefinition save(CustomAttributeDefinition definition) {
        return CustomAttributeDefinitionMapper.toDomain(
                springDataCustomAttributeDefinitionRepository.save(CustomAttributeDefinitionMapper.toEntity(definition))
        );
    }

    @Override
    public Optional<CustomAttributeDefinition> findByTenantIdAndName(UUID tenantId, String name) {
        return springDataCustomAttributeDefinitionRepository.findByTenant_IdAndName(tenantId, name)
                .map(CustomAttributeDefinitionMapper::toDomain);
    }

    @Override
    public List<CustomAttributeDefinition> findByTenantId(UUID tenantId) {
        return springDataCustomAttributeDefinitionRepository.findByTenant_Id(tenantId).stream()
                .map(CustomAttributeDefinitionMapper::toDomain)
                .toList();
    }

    @Override
    public List<CustomAttributeDefinition> findRequiredByTenantId(UUID tenantId) {
        return springDataCustomAttributeDefinitionRepository.findByTenant_IdAndRequiredTrue(tenantId).stream()
                .map(CustomAttributeDefinitionMapper::toDomain)
                .toList();
    }

    @Override
    public List<CustomAttributeDefinition> findByIdsAndTenantId(Collection<UUID> ids, UUID tenantId) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        return springDataCustomAttributeDefinitionRepository.findByIdInAndTenant_Id(ids, tenantId).stream()
                .map(CustomAttributeDefinitionMapper::toDomain)
                .toList();
    }
}
