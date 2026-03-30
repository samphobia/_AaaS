package com.authservice.application.service;

import com.authservice.application.exception.BadRequestException;
import com.authservice.application.exception.NotFoundException;
import com.authservice.domain.model.AttributeType;
import com.authservice.domain.model.AuthUser;
import com.authservice.domain.model.CustomAttributeDefinition;
import com.authservice.domain.model.UserAttributeValue;
import com.authservice.domain.repository.AuthUserRepository;
import com.authservice.domain.repository.CustomAttributeDefinitionRepository;
import com.authservice.domain.repository.UserAttributeValueRepository;
import com.authservice.infrastructure.security.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomAttributeApplicationServiceTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private CustomAttributeDefinitionRepository customAttributeDefinitionRepository;

    @Mock
    private UserAttributeValueRepository userAttributeValueRepository;

    @InjectMocks
    private CustomAttributeApplicationService service;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void createDefinition_ShouldPersistDefinitionInTenant() {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        when(customAttributeDefinitionRepository.findByTenantIdAndName(tenantId, "department")).thenReturn(Optional.empty());
        when(customAttributeDefinitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CustomAttributeDefinition saved = service.createDefinition("department", AttributeType.STRING, true);

        assertThat(saved.getTenantId()).isEqualTo(tenantId);
        assertThat(saved.getName()).isEqualTo("department");
        assertThat(saved.getType()).isEqualTo(AttributeType.STRING);
        assertThat(saved.isRequired()).isTrue();
    }

    @Test
    void assignUserAttributes_ShouldValidateNumberType() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID definitionId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        when(authUserRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user(userId, tenantId)));
        when(customAttributeDefinitionRepository.findByTenantId(tenantId)).thenReturn(List.of(
                CustomAttributeDefinition.builder()
                        .id(definitionId)
                        .tenantId(tenantId)
                        .name("age")
                        .type(AttributeType.NUMBER)
                        .required(false)
                        .build()
        ));
        when(userAttributeValueRepository.findByUserId(userId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.assignUserAttributes(userId, Map.of("age", "not-a-number")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expects NUMBER");
    }

    @Test
    void assignUserAttributes_ShouldEnforceRequiredAttributes() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID requiredId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        when(authUserRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user(userId, tenantId)));
        when(customAttributeDefinitionRepository.findByTenantId(tenantId)).thenReturn(List.of(
                CustomAttributeDefinition.builder()
                        .id(requiredId)
                        .tenantId(tenantId)
                        .name("department")
                        .type(AttributeType.STRING)
                        .required(true)
                        .build()
        ));
        when(userAttributeValueRepository.findByUserId(userId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.assignUserAttributes(userId, Map.of("department", "")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("required");
    }

    @Test
    void getUserAttributes_ShouldReturnTenantScopedKeyValueMap() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID depDefId = UUID.randomUUID();
        UUID activeDefId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        when(authUserRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user(userId, tenantId)));
        when(userAttributeValueRepository.findByUserId(userId)).thenReturn(List.of(
                UserAttributeValue.builder().id(UUID.randomUUID()).userId(userId).attributeId(depDefId).value("sales").build(),
                UserAttributeValue.builder().id(UUID.randomUUID()).userId(userId).attributeId(activeDefId).value("true").build()
        ));
        when(customAttributeDefinitionRepository.findByIdsAndTenantId(Set.of(depDefId, activeDefId), tenantId)).thenReturn(List.of(
                CustomAttributeDefinition.builder().id(depDefId).tenantId(tenantId).name("department").type(AttributeType.STRING).required(false).build(),
                CustomAttributeDefinition.builder().id(activeDefId).tenantId(tenantId).name("active").type(AttributeType.BOOLEAN).required(false).build()
        ));

        Map<String, Object> attributes = service.getUserAttributes(userId);

        assertThat(attributes)
                .containsEntry("department", "sales")
                .containsEntry("active", true);
    }

    @Test
    void getUserAttributes_ShouldRejectCrossTenantAccess() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        when(authUserRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUserAttributes(userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void assignUserAttributesForSignup_ShouldAllowEmptyPayload_WhenNoRequiredDefinitions() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        when(authUserRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user(userId, tenantId)));
        when(customAttributeDefinitionRepository.findByTenantId(tenantId)).thenReturn(List.of());
        when(customAttributeDefinitionRepository.findRequiredByTenantId(tenantId)).thenReturn(List.of());
        when(userAttributeValueRepository.findByUserId(userId)).thenReturn(List.of());

        Map<String, Object> result = service.assignUserAttributesForSignup(userId, Map.of());

        assertThat(result).isEmpty();
    }

    @Test
    void assignUserAttributesForSignup_ShouldEnforceRequiredDefinitions() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID requiredId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        when(authUserRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user(userId, tenantId)));
        when(customAttributeDefinitionRepository.findByTenantId(tenantId)).thenReturn(List.of(
                CustomAttributeDefinition.builder()
                        .id(requiredId)
                        .tenantId(tenantId)
                        .name("idcard")
                        .type(AttributeType.STRING)
                        .required(true)
                        .build()
        ));
        when(customAttributeDefinitionRepository.findRequiredByTenantId(tenantId)).thenReturn(List.of(
                CustomAttributeDefinition.builder()
                        .id(requiredId)
                        .tenantId(tenantId)
                        .name("idcard")
                        .type(AttributeType.STRING)
                        .required(true)
                        .build()
        ));
        when(userAttributeValueRepository.findByUserId(userId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.assignUserAttributesForSignup(userId, Map.of()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("idcard");
    }

    private AuthUser user(UUID userId, UUID tenantId) {
        return AuthUser.builder()
                .id(userId)
                .keycloakUserId("kc-user")
                .externalUserId("ext")
                .tenantId(tenantId)
                .roles(Set.of(com.authservice.domain.model.Role.USER))
                .active(true)
                .createdAt(Instant.now())
                .build();
    }
}
