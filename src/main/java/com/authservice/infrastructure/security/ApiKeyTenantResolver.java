package com.authservice.infrastructure.security;

import com.authservice.application.exception.UnauthorizedException;
import com.authservice.domain.model.Tenant;
import com.authservice.domain.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "auth", name = "mode", havingValue = "MULTI_TENANT", matchIfMissing = true)
public class ApiKeyTenantResolver implements TenantResolver {

    private final TenantRepository tenantRepository;

    @Override
    public Tenant resolve(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new UnauthorizedException("Missing X-API-KEY header");
        }
        return tenantRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new UnauthorizedException("Invalid API key"));
    }
}