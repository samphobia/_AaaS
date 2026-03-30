package com.authservice.application.service;

import com.authservice.application.exception.BadRequestException;
import com.authservice.application.exception.ConflictException;
import com.authservice.application.service.model.TenantProvisioningResult;
import com.authservice.domain.model.AttributeType;
import com.authservice.domain.model.CustomAttributeDefinition;
import com.authservice.domain.model.Tenant;
import com.authservice.domain.repository.CustomAttributeDefinitionRepository;
import com.authservice.domain.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantOnboardingApplicationServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private CustomAttributeDefinitionRepository customAttributeDefinitionRepository;

    @InjectMocks
    private TenantOnboardingApplicationService service;

    @Test
    void createTenant_ShouldCreateTenantAndAttributeSchema() {
        when(tenantRepository.findByApiKey("tenant-a-key")).thenReturn(Optional.empty());
        when(tenantRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(customAttributeDefinitionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TenantProvisioningResult result = service.createTenant(
                "Tenant A",
                "tenant-a-key",
                List.of(
                        TenantOnboardingApplicationService.AttributeDefinitionInput.builder()
                                .name("Name")
                                .type(AttributeType.STRING)
                                .required(true)
                                .build(),
                        TenantOnboardingApplicationService.AttributeDefinitionInput.builder()
                                .name("Income")
                                .type(AttributeType.NUMBER)
                                .required(false)
                                .build()
                )
        );

        assertThat(result.getTenant().getName()).isEqualTo("Tenant A");
        assertThat(result.getTenant().getApiKey()).isEqualTo("tenant-a-key");
        assertThat(result.getAttributeDefinitions()).hasSize(2);
        assertThat(result.getAttributeDefinitions().stream().map(CustomAttributeDefinition::getName))
                .containsExactlyInAnyOrder("name", "income");
    }

    @Test
    void createTenant_ShouldRejectDuplicateApiKey() {
        when(tenantRepository.findByApiKey("existing-key")).thenReturn(Optional.of(
                Tenant.builder().id(UUID.randomUUID()).name("Existing").apiKey("existing-key").build()
        ));

        assertThatThrownBy(() -> service.createTenant("Tenant B", "existing-key", List.of()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("apiKey already exists");
    }

    @Test
    void createTenant_ShouldRejectDuplicateAttributeNamesInRequest() {
        when(tenantRepository.findByApiKey("tenant-b-key")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createTenant(
                "Tenant B",
                "tenant-b-key",
                List.of(
                        TenantOnboardingApplicationService.AttributeDefinitionInput.builder()
                                .name("Name")
                                .type(AttributeType.STRING)
                                .required(true)
                                .build(),
                        TenantOnboardingApplicationService.AttributeDefinitionInput.builder()
                                .name("name")
                                .type(AttributeType.STRING)
                                .required(false)
                                .build()
                )
        )).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Duplicate attribute name");
    }
}
