package com.authservice.interfaces.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LoginResponse {
    String accessToken;
    String refreshToken;
    long expiresIn;
}
