package com.authservice.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class AuthMetadata {
    UUID userId;
    Instant lastLoginAt;
    String lastLoginIp;
    String device;
}
