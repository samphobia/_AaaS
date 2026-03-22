package com.authservice.infrastructure.persistence.repository;

import com.authservice.infrastructure.persistence.entity.AuthMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataAuthMetadataRepository extends JpaRepository<AuthMetadataEntity, UUID> {
}
