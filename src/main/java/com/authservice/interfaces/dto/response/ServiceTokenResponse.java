package com.authservice.interfaces.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ServiceTokenResponse {
    String accessToken;
    long expiresIn;
    String tokenType;
}
