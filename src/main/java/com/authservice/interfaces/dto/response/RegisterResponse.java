package com.authservice.interfaces.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class RegisterResponse {
    UUID userId;
    String keycloakUserId;
    String externalUserId;
    UUID tenantId;
    String status;
}
