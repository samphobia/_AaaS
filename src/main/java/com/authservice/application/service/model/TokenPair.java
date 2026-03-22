package com.authservice.application.service.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TokenPair {
    String accessToken;
    String refreshToken;
    long expiresIn;
}
