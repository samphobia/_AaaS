package com.authservice.interfaces.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Value
@Builder
public class ServicePrincipalResponse {
    String principal;
    Set<String> scopes;
}
