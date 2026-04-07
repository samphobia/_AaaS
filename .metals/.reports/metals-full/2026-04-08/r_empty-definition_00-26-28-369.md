error id: file://<WORKSPACE>/src/main/java/com/authservice/application/service/AuthApplicationService.java:_empty_/`<any>`#active#
file://<WORKSPACE>/src/main/java/com/authservice/application/service/AuthApplicationService.java
empty definition using pc, found symbol in pc: _empty_/`<any>`#active#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 2704
uri: file://<WORKSPACE>/src/main/java/com/authservice/application/service/AuthApplicationService.java
text:
```scala
package com.authservice.application.service;

import com.authservice.application.exception.ConflictException;
import com.authservice.application.exception.NotFoundException;
import com.authservice.application.service.model.TokenPair;
import com.authservice.application.usecase.GetCurrentUserUseCase;
import com.authservice.application.usecase.LoginUseCase;
import com.authservice.application.usecase.RefreshTokenUseCase;
import com.authservice.application.usecase.RegisterUserUseCase;
import com.authservice.application.usecase.command.LoginCommand;
import com.authservice.application.usecase.command.RefreshTokenCommand;
import com.authservice.application.usecase.command.RegisterUserCommand;
import com.authservice.domain.model.AuthUser;
import com.authservice.domain.model.Role;
import com.authservice.domain.model.Tenant;
import com.authservice.domain.repository.AuthUserRepository;
import com.authservice.domain.repository.TenantRepository;
import com.authservice.infrastructure.security.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthApplicationService implements RegisterUserUseCase, LoginUseCase, RefreshTokenUseCase, GetCurrentUserUseCase {

    private final AuthUserRepository authUserRepository;
    private final TenantRepository tenantRepository;
    private final IdentityProviderClient identityProviderClient;
    private final AuditLogService auditLogService;
    private final CustomAttributeApplicationService customAttributeApplicationService;

    @Override
    @Transactional
    public AuthUser register(RegisterUserCommand command) {
        Tenant tenant = tenantRepository.findByApiKey(command.getApiKey())
                .orElseThrow(() -> new NotFoundException("Tenant not found for given API key"));

        authUserRepository.findByExternalUserIdAndTenantId(command.getExternalUserId(), tenant.getId())
                .ifPresent(existing -> {
                    throw new ConflictException("externalUserId already mapped in tenant");
                });

        String keycloakUserId = identityProviderClient.createUser(command.getEmail(), command.getPassword());

        AuthUser user = AuthUser.builder()
                .id(UUID.randomUUID())
                .keycloakUserId(keycloakUserId)
                .externalUserId(command.getExternalUserId())
                .roles(Set.of(Role.USER))
                .tenantId(tenant.getId())
                .@@active(true)
                .createdAt(Instant.now())
                .build();

        log.info("Registered user mapping externalUserId={} tenantId={}", command.getExternalUserId(), tenant.getId());
        AuthUser saved = authUserRepository.save(user);
        customAttributeApplicationService.assignUserAttributesForSignup(saved.getId(), defaultAttributes(command.getAttributes()));
        auditLogService.logEvent("REGISTER", "SUCCESS", tenant.getId(), keycloakUserId, "externalUserId=" + command.getExternalUserId());
        return saved;
    }

    private Map<String, Object> defaultAttributes(Map<String, Object> attributes) {
        return attributes == null ? Map.of() : attributes;
    }

    @Override
    public TokenPair login(LoginCommand command) {
        TokenPair tokenPair = identityProviderClient.getToken(command.getUsername(), command.getPassword());
        auditLogService.logEvent("LOGIN", "SUCCESS", TenantContextHolder.getRequiredTenantId(), command.getUsername(), "token_issued=true");
        return tokenPair;
    }

    @Override
    public TokenPair refresh(RefreshTokenCommand command) {
        TokenPair tokenPair = identityProviderClient.refreshToken(command.getRefreshToken());
        auditLogService.logEvent("REFRESH", "SUCCESS", TenantContextHolder.getRequiredTenantId(), "anonymous", "token_refreshed=true");
        return tokenPair;
    }

    public TokenPair issueServiceToken(String clientId, String clientSecret, String scope) {
        TokenPair tokenPair = identityProviderClient.getServiceToken(clientId, clientSecret, scope);
        auditLogService.logEvent(
                "SERVICE_TOKEN",
                "SUCCESS",
                TenantContextHolder.getRequiredTenantId(),
                clientId,
                "scope=" + (scope == null ? "" : scope)
        );
        return tokenPair;
    }

    public void forgotPassword(String email) {
        identityProviderClient.initiatePasswordReset(email);
        auditLogService.logEvent(
                "FORGOT_PASSWORD",
                "SUCCESS",
                TenantContextHolder.getRequiredTenantId(),
                email,
                "reset_email_requested=true"
        );
    }

    public void changePassword(String keycloakUserId, String accessToken, String currentPassword, String newPassword) {
        getCurrentUser(keycloakUserId);
        identityProviderClient.changePassword(accessToken, currentPassword, newPassword);
        auditLogService.logEvent(
                "CHANGE_PASSWORD",
                "SUCCESS",
                TenantContextHolder.getRequiredTenantId(),
                keycloakUserId,
                "password_changed=true"
        );
    }

    @Override
    public AuthUser getCurrentUser(String keycloakUserId) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        AuthUser authUser = authUserRepository.findByKeycloakUserIdAndTenantId(keycloakUserId, tenantId)
                .orElseThrow(() -> new NotFoundException("Auth user mapping not found"));
        auditLogService.logEvent("ME", "SUCCESS", tenantId, keycloakUserId, "mapping_found=true");
        return authUser;
    }
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: _empty_/`<any>`#active#