package com.authservice.application.service;

import com.authservice.application.service.model.TokenPair;
import com.authservice.application.service.model.TokenValidationResult;

public interface IdentityProviderClient {

    String createUser(String email, String password);

    TokenPair getToken(String username, String password);

    TokenPair refreshToken(String refreshToken);

    TokenPair getServiceToken(String clientId, String clientSecret, String scope);

    TokenValidationResult validateToken(String token);
}
