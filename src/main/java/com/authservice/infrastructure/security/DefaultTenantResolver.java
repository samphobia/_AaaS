package com.authservice.infrastructure.security;

import com.authservice.application.exception.UnauthorizedException;
import com.authservice.config.AuthProperties;
import com.authservice.domain.model.Tenant;
import com.authservice.domain.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "auth", name = "mode", havingValue = "STANDALONE")
public class DefaultTenantResolver implements TenantResolver {

    private final TenantRepository tenantRepository;
    private final AuthProperties authProperties;

    @Override
    public Tenant resolve(String apiKey) {
        String defaultTenantApiKey = authProperties.getDefaultTenantApiKey();
        return tenantRepository.findByApiKey(defaultTenantApiKey)
                .orElseThrow(() -> new UnauthorizedException("Default tenant is not configured"));
    }
}