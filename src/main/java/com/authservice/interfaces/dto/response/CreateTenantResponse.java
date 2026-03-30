package com.authservice.interfaces.dto.response;

import com.authservice.domain.model.AttributeType;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class CreateTenantResponse {
    UUID tenantId;
    String name;
    String apiKey;
    List<AttributeDefinition> attributes;

    @Value
    @Builder
    public static class AttributeDefinition {
        UUID id;
        String name;
        AttributeType type;
        boolean required;
    }
}
