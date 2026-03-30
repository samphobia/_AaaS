package com.authservice.application.service;

import com.authservice.application.exception.ConflictException;
import com.authservice.application.exception.NotFoundException;
import com.authservice.application.service.model.TokenPair;
import com.authservice.application.usecase.command.RegisterUserCommand;
import com.authservice.domain.model.AuthUser;
import com.authservice.domain.model.Tenant;
import com.authservice.domain.repository.AuthUserRepository;
import com.authservice.domain.repository.TenantRepository;
import com.authservice.infrastructure.security.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthApplicationServiceTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private IdentityProviderClient identityProviderClient;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private CustomAttributeApplicationService customAttributeApplicationService;

    @InjectMocks
    private AuthApplicationService authApplicationService;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void register_ShouldCreateMapping_WhenInputIsValid() {
        UUID tenantId = UUID.randomUUID();
        when(tenantRepository.findByApiKey("api-key")).thenReturn(Optional.of(Tenant.builder()
                .id(tenantId)
                .name("Tenant")
                .apiKey("api-key")
                .build()));
        when(authUserRepository.findByExternalUserIdAndTenantId("ext-1", tenantId)).thenReturn(Optional.empty());
        when(identityProviderClient.createUser("test@mail.com", "StrongPass123")).thenReturn("kc-123");
        when(authUserRepository.save(any(AuthUser.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthUser result = authApplicationService.register(RegisterUserCommand.builder()
                .email("test@mail.com")
                .password("StrongPass123")
                .externalUserId("ext-1")
                .apiKey("api-key")
            .attributes(java.util.Map.of("name", "John"))
                .build());

        assertThat(result.getKeycloakUserId()).isEqualTo("kc-123");
        assertThat(result.getTenantId()).isEqualTo(tenantId);
        assertThat(result.getRoles()).isEqualTo(Set.of(com.authservice.domain.model.Role.USER));
        verify(customAttributeApplicationService).assignUserAttributesForSignup(result.getId(), java.util.Map.of("name", "John"));
    }

    @Test
    void register_ShouldThrowConflict_WhenExternalUserAlreadyMapped() {
        UUID tenantId = UUID.randomUUID();
        when(tenantRepository.findByApiKey("api-key")).thenReturn(Optional.of(Tenant.builder()
                .id(tenantId)
                .name("Tenant")
                .apiKey("api-key")
                .build()));
        when(authUserRepository.findByExternalUserIdAndTenantId("ext-1", tenantId)).thenReturn(Optional.of(AuthUser.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .keycloakUserId("kc")
                .externalUserId("ext-1")
                .active(true)
                .build()));

        assertThatThrownBy(() -> authApplicationService.register(RegisterUserCommand.builder()
                .email("test@mail.com")
                .password("StrongPass123")
                .externalUserId("ext-1")
                .apiKey("api-key")
                .build())).isInstanceOf(ConflictException.class);
    }

    @Test
    void getCurrentUser_ShouldThrow_WhenTenantContextMissing() {
        assertThatThrownBy(() -> authApplicationService.getCurrentUser("kc-1"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void getCurrentUser_ShouldThrowNotFound_WhenMappingMissing() {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        when(authUserRepository.findByKeycloakUserIdAndTenantId("kc-1", tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authApplicationService.getCurrentUser("kc-1"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void issueServiceToken_ShouldDelegateToIdentityProvider() {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        when(identityProviderClient.getServiceToken(eq("auth-client"), eq("auth-client-secret"), eq("service.read")))
                .thenReturn(TokenPair.builder()
                        .accessToken("service-token")
                        .expiresIn(300)
                        .build());

        TokenPair tokenPair = authApplicationService.issueServiceToken("auth-client", "auth-client-secret", "service.read");

        assertThat(tokenPair.getAccessToken()).isEqualTo("service-token");
        assertThat(tokenPair.getExpiresIn()).isEqualTo(300);
    }
}
