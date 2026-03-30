package com.authservice.application.service;

import com.authservice.application.exception.BadRequestException;
import com.authservice.application.exception.ConflictException;
import com.authservice.application.exception.NotFoundException;
import com.authservice.domain.model.AttributeType;
import com.authservice.domain.model.AuthUser;
import com.authservice.domain.model.CustomAttributeDefinition;
import com.authservice.domain.model.UserAttributeValue;
import com.authservice.domain.repository.AuthUserRepository;
import com.authservice.domain.repository.CustomAttributeDefinitionRepository;
import com.authservice.domain.repository.UserAttributeValueRepository;
import com.authservice.infrastructure.security.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomAttributeApplicationService {

    private final AuthUserRepository authUserRepository;
    private final CustomAttributeDefinitionRepository customAttributeDefinitionRepository;
    private final UserAttributeValueRepository userAttributeValueRepository;

    @Transactional
    public CustomAttributeDefinition createDefinition(String name, AttributeType type, boolean required) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        String normalizedName = normalizeName(name);

        customAttributeDefinitionRepository.findByTenantIdAndName(tenantId, normalizedName)
                .ifPresent(existing -> {
                    throw new ConflictException("Attribute definition already exists for tenant");
                });

        CustomAttributeDefinition definition = CustomAttributeDefinition.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .name(normalizedName)
                .type(type)
                .required(required)
                .build();
        return customAttributeDefinitionRepository.save(definition);
    }

    @Transactional
    public Map<String, Object> assignUserAttributes(UUID userId, Map<String, Object> attributes) {
        return assignUserAttributesInternal(userId, attributes, false);
    }

    @Transactional
    public Map<String, Object> assignUserAttributesForSignup(UUID userId, Map<String, Object> attributes) {
        return assignUserAttributesInternal(userId, attributes, true);
    }

    private Map<String, Object> assignUserAttributesInternal(UUID userId,
                                                             Map<String, Object> attributes,
                                                             boolean allowEmptyPayload) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        AuthUser user = authUserRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new NotFoundException("User not found in tenant"));

        Map<String, Object> incomingAttributes = attributes == null ? Map.of() : attributes;
        if (!allowEmptyPayload && incomingAttributes.isEmpty()) {
            throw new BadRequestException("Attributes payload cannot be empty");
        }

        Map<String, CustomAttributeDefinition> definitionsByName = customAttributeDefinitionRepository.findByTenantId(tenantId)
                .stream()
                .collect(Collectors.toMap(CustomAttributeDefinition::getName, definition -> definition));

        List<UserAttributeValue> existingValues = userAttributeValueRepository.findByUserId(user.getId());
        Map<UUID, UserAttributeValue> existingValuesByAttributeId = existingValues.stream()
                .collect(Collectors.toMap(UserAttributeValue::getAttributeId, value -> value));

        Map<UUID, String> mergedValues = existingValues.stream()
                .collect(Collectors.toMap(UserAttributeValue::getAttributeId, UserAttributeValue::getValue));

        for (Map.Entry<String, Object> entry : incomingAttributes.entrySet()) {
            String normalizedName = normalizeName(entry.getKey());
            CustomAttributeDefinition definition = definitionsByName.get(normalizedName);
            if (definition == null) {
                throw new NotFoundException("Unknown attribute definition: " + normalizedName);
            }

            String serializedValue = serializeAndValidateValue(definition, entry.getValue());
            if (serializedValue == null) {
                mergedValues.remove(definition.getId());
            } else {
                mergedValues.put(definition.getId(), serializedValue);
            }
        }

        enforceRequiredAttributes(tenantId, mergedValues);

        List<UserAttributeValue> toSave = buildUpserts(user.getId(), mergedValues, existingValuesByAttributeId);
        List<UserAttributeValue> toDelete = existingValues.stream()
                .filter(existing -> !mergedValues.containsKey(existing.getAttributeId()))
                .toList();

        userAttributeValueRepository.saveAll(toSave);
        userAttributeValueRepository.deleteAll(toDelete);

        Map<UUID, CustomAttributeDefinition> definitionsById = definitionsByName.values().stream()
            .collect(Collectors.toMap(CustomAttributeDefinition::getId, definition -> definition));
        return buildTypedAttributeMap(mergedValues, definitionsById);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserAttributes(UUID userId) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        authUserRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new NotFoundException("User not found in tenant"));

        List<UserAttributeValue> values = userAttributeValueRepository.findByUserId(userId);
        if (values.isEmpty()) {
            return Map.of();
        }

        Set<UUID> definitionIds = values.stream()
                .map(UserAttributeValue::getAttributeId)
                .collect(Collectors.toSet());

        Map<UUID, CustomAttributeDefinition> definitionsById = customAttributeDefinitionRepository
                .findByIdsAndTenantId(definitionIds, tenantId)
                .stream()
                .collect(Collectors.toMap(CustomAttributeDefinition::getId, definition -> definition));

        Map<String, Object> result = new HashMap<>();
        for (UserAttributeValue value : values) {
            CustomAttributeDefinition definition = definitionsById.get(value.getAttributeId());
            if (definition == null) {
                continue;
            }
            result.put(definition.getName(), deserializeValue(definition.getType(), value.getValue()));
        }

        return Map.copyOf(result);
    }

    private Map<String, Object> buildTypedAttributeMap(Map<UUID, String> valuesByAttributeId,
                                                       Map<UUID, CustomAttributeDefinition> definitionsById) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<UUID, String> entry : valuesByAttributeId.entrySet()) {
            CustomAttributeDefinition definition = definitionsById.get(entry.getKey());
            if (definition == null) {
                continue;
            }
            result.put(definition.getName(), deserializeValue(definition.getType(), entry.getValue()));
        }
        return Map.copyOf(result);
    }

    private void enforceRequiredAttributes(UUID tenantId, Map<UUID, String> mergedValues) {
        List<CustomAttributeDefinition> requiredDefinitions = customAttributeDefinitionRepository.findRequiredByTenantId(tenantId);
        for (CustomAttributeDefinition requiredDefinition : requiredDefinitions) {
            String requiredValue = mergedValues.get(requiredDefinition.getId());
            if (requiredValue == null || requiredValue.isBlank()) {
                throw new BadRequestException("Missing required attribute: " + requiredDefinition.getName());
            }
        }
    }

    private List<UserAttributeValue> buildUpserts(UUID userId,
                                                  Map<UUID, String> mergedValues,
                                                  Map<UUID, UserAttributeValue> existingValuesByAttributeId) {
        return mergedValues.entrySet().stream()
                .map(entry -> {
                    UserAttributeValue existing = existingValuesByAttributeId.get(entry.getKey());
                    if (existing != null) {
                        return existing.toBuilder().value(entry.getValue()).build();
                    }
                    return UserAttributeValue.builder()
                            .id(UUID.randomUUID())
                            .userId(userId)
                            .attributeId(entry.getKey())
                            .value(entry.getValue())
                            .build();
                })
                .toList();
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Attribute name is required");
        }
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private String serializeAndValidateValue(CustomAttributeDefinition definition, Object rawValue) {
        if (rawValue == null) {
            return null;
        }

        return switch (definition.getType()) {
            case STRING -> serializeString(definition, rawValue);
            case NUMBER -> serializeNumber(definition, rawValue);
            case BOOLEAN -> serializeBoolean(definition, rawValue);
        };
    }

    private String serializeString(CustomAttributeDefinition definition, Object rawValue) {
        if (!(rawValue instanceof String value)) {
            throw new BadRequestException("Attribute " + definition.getName() + " expects STRING");
        }
        if (definition.isRequired() && value.isBlank()) {
            throw new BadRequestException("Attribute " + definition.getName() + " is required");
        }
        return value;
    }

    private String serializeNumber(CustomAttributeDefinition definition, Object rawValue) {
        try {
            BigDecimal number;
            if (rawValue instanceof Number n) {
                number = new BigDecimal(n.toString());
            } else if (rawValue instanceof String s) {
                number = new BigDecimal(s);
            } else {
                throw new NumberFormatException("Unsupported type");
            }
            return number.stripTrailingZeros().toPlainString();
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Attribute " + definition.getName() + " expects NUMBER");
        }
    }

    private String serializeBoolean(CustomAttributeDefinition definition, Object rawValue) {
        if (rawValue instanceof Boolean b) {
            return Boolean.toString(b);
        }
        if (rawValue instanceof String s && ("true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s))) {
            return s.toLowerCase(Locale.ROOT);
        }
        throw new BadRequestException("Attribute " + definition.getName() + " expects BOOLEAN");
    }

    private Object deserializeValue(AttributeType type, String value) {
        return switch (type) {
            case STRING -> value;
            case NUMBER -> new BigDecimal(value);
            case BOOLEAN -> Boolean.parseBoolean(value);
        };
    }
}
