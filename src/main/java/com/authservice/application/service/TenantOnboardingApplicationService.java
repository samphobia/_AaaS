package com.authservice.application.service;

import com.authservice.application.exception.BadRequestException;
import com.authservice.application.exception.ConflictException;
import com.authservice.application.service.model.TenantProvisioningResult;
import com.authservice.domain.model.AttributeType;
import com.authservice.domain.model.CustomAttributeDefinition;
import com.authservice.domain.model.Tenant;
import com.authservice.domain.repository.CustomAttributeDefinitionRepository;
import com.authservice.domain.repository.TenantRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantOnboardingApplicationService {

    private final TenantRepository tenantRepository;
    private final CustomAttributeDefinitionRepository customAttributeDefinitionRepository;

    @Transactional
    public TenantProvisioningResult createTenant(String name, String apiKey, List<AttributeDefinitionInput> attributeDefinitions) {
        String normalizedName = normalizeTenantField(name, "Tenant name is required");
        String normalizedApiKey = normalizeTenantField(apiKey, "Tenant apiKey is required");

        tenantRepository.findByApiKey(normalizedApiKey)
                .ifPresent(existing -> {
                    throw new ConflictException("Tenant apiKey already exists");
                });

        List<AttributeDefinitionInput> requestedDefinitions = attributeDefinitions == null ? List.of() : attributeDefinitions;
        validateUniqueAttributeNames(requestedDefinitions);

        Tenant savedTenant = tenantRepository.save(Tenant.builder()
                .id(UUID.randomUUID())
                .name(normalizedName)
                .apiKey(normalizedApiKey)
                .build());

        List<CustomAttributeDefinition> savedDefinitions = requestedDefinitions.stream()
                .map(input -> customAttributeDefinitionRepository.save(CustomAttributeDefinition.builder()
                        .id(UUID.randomUUID())
                        .tenantId(savedTenant.getId())
                        .name(normalizeAttributeName(input.getName()))
                        .type(input.getType())
                        .required(input.isRequired())
                        .build()))
                .toList();

        return TenantProvisioningResult.builder()
                .tenant(savedTenant)
                .attributeDefinitions(savedDefinitions)
                .build();
    }

    private void validateUniqueAttributeNames(List<AttributeDefinitionInput> definitions) {
        Set<String> normalizedNames = new HashSet<>();
        for (AttributeDefinitionInput definition : definitions) {
            String normalizedName = normalizeAttributeName(definition.getName());
            if (!normalizedNames.add(normalizedName)) {
                throw new BadRequestException("Duplicate attribute name in request: " + normalizedName);
            }
        }
    }

    private String normalizeTenantField(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value.trim();
    }

    private String normalizeAttributeName(String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Attribute name is required");
        }
        return name.trim().toLowerCase(Locale.ROOT);
    }

    @Value
    @Builder
    public static class AttributeDefinitionInput {
        String name;
        AttributeType type;
        boolean required;
    }
}
