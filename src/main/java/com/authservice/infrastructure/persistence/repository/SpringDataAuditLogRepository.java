package com.authservice.infrastructure.persistence.repository;

import com.authservice.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataAuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {
}
