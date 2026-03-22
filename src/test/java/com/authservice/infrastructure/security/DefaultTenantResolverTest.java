package com.authservice.infrastructure.security;

import com.authservice.application.exception.UnauthorizedException;
import com.authservice.config.AuthProperties;
import com.authservice.domain.model.Tenant;
import com.authservice.domain.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultTenantResolverTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private AuthProperties authProperties;

    @InjectMocks
    private DefaultTenantResolver resolver;

    @Test
    void resolve_ShouldReturnDefaultTenant_IgnoringApiKey() {
        Tenant tenant = Tenant.builder()
                .id(UUID.randomUUID())
                .name("Default Tenant")
                .apiKey("default-key")
                .build();
        when(authProperties.getDefaultTenantApiKey()).thenReturn("default-key");
        when(tenantRepository.findByApiKey("default-key")).thenReturn(Optional.of(tenant));

        Tenant resolved = resolver.resolve(null);

        assertThat(resolved).isEqualTo(tenant);
    }

    @Test
    void resolve_ShouldThrow_WhenDefaultTenantMissing() {
        when(authProperties.getDefaultTenantApiKey()).thenReturn("missing-key");
        when(tenantRepository.findByApiKey("missing-key")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve("ignored"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Default tenant is not configured");
    }
}
