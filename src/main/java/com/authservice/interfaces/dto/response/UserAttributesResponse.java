package com.authservice.interfaces.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.Map;
import java.util.UUID;

@Value
@Builder
public class UserAttributesResponse {
    UUID userId;
    Map<String, Object> attributes;
}
