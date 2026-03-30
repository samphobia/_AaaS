package com.authservice.domain.model;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class CustomAttributeDefinition {
    UUID id;
    UUID tenantId;
    String name;
    AttributeType type;
    boolean required;
}
