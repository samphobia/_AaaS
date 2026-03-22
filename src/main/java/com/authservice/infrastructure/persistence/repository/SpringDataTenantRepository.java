package com.authservice.infrastructure.persistence.repository;

import com.authservice.infrastructure.persistence.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataTenantRepository extends JpaRepository<TenantEntity, UUID> {

    Optional<TenantEntity> findByApiKey(String apiKey);
}
