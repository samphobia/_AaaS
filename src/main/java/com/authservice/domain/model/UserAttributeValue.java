package com.authservice.domain.model;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class UserAttributeValue {
    UUID id;
    UUID userId;
    UUID attributeId;
    String value;
}
