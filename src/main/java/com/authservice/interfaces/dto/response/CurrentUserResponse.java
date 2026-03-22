package com.authservice.interfaces.dto.response;

import com.authservice.domain.model.Role;
import lombok.Builder;
import lombok.Value;

import java.util.Set;
import java.util.UUID;

@Value
@Builder
public class CurrentUserResponse {
    String externalUserId;
    UUID tenantId;
    Set<Role> roles;
}
