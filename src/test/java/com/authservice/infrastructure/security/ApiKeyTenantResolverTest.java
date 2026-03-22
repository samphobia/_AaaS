package com.authservice.infrastructure.security;

import com.authservice.application.exception.UnauthorizedException;
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
class ApiKeyTenantResolverTest {

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private ApiKeyTenantResolver resolver;

    @Test
    void resolve_ShouldReturnTenant_WhenApiKeyIsValid() {
        Tenant tenant = Tenant.builder()
                .id(UUID.randomUUID())
                .name("Tenant")
                .apiKey("api-key")
                .build();
        when(tenantRepository.findByApiKey("api-key")).thenReturn(Optional.of(tenant));

        Tenant resolved = resolver.resolve("api-key");

        assertThat(resolved).isEqualTo(tenant);
    }

    @Test
    void resolve_ShouldThrow_WhenApiKeyMissing() {
        assertThatThrownBy(() -> resolver.resolve(null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Missing X-API-KEY");
    }

    @Test
    void resolve_ShouldThrow_WhenApiKeyUnknown() {
        when(tenantRepository.findByApiKey("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve("unknown"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid API key");
    }
}
