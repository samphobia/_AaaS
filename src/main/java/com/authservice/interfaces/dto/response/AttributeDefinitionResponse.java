package com.authservice.interfaces.dto.response;

import com.authservice.domain.model.AttributeType;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class AttributeDefinitionResponse {
    UUID id;
    String name;
    AttributeType type;
    boolean required;
}
