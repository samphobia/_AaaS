package com.authservice.application.service.model;

import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Value
@Builder
public class TokenValidationResult {
    boolean active;
    String subject;
    String username;
    String clientId;
    boolean machineToken;
    Set<String> roles;
    Set<String> scopes;
}
