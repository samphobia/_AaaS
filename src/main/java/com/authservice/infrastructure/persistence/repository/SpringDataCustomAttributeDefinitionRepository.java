package com.authservice.infrastructure.persistence.repository;

import com.authservice.infrastructure.persistence.entity.CustomAttributeDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataCustomAttributeDefinitionRepository extends JpaRepository<CustomAttributeDefinitionEntity, UUID> {

    Optional<CustomAttributeDefinitionEntity> findByTenant_IdAndName(UUID tenantId, String name);

    List<CustomAttributeDefinitionEntity> findByTenant_Id(UUID tenantId);

    List<CustomAttributeDefinitionEntity> findByTenant_IdAndRequiredTrue(UUID tenantId);

    List<CustomAttributeDefinitionEntity> findByIdInAndTenant_Id(Collection<UUID> ids, UUID tenantId);
}
