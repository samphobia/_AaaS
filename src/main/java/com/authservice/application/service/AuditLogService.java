package com.authservice.application.service;

import com.authservice.infrastructure.persistence.entity.AuditLogEntity;
import com.authservice.infrastructure.persistence.repository.SpringDataAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final SpringDataAuditLogRepository springDataAuditLogRepository;

    public void logEvent(String eventType, String outcome, UUID tenantId, String principal, String details) {
        springDataAuditLogRepository.save(AuditLogEntity.builder()
                .eventType(eventType)
                .outcome(outcome)
                .tenantId(tenantId == null ? "N/A" : tenantId.toString())
                .principal(principal == null ? "anonymous" : principal)
                .createdAt(Instant.now())
                .details(details)
                .build());
        log.info("audit event={} outcome={} tenant={} principal={}", eventType, outcome, tenantId, principal);
    }
}
