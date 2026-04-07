package com.authservice.application.service;

import com.authservice.application.service.model.TokenPair;
import com.authservice.application.service.model.TokenValidationResult;

import java.util.Map;
import java.util.Set;

public interface IdentityProviderClient {

    String createUser(String email, String password);

    TokenPair getToken(String username, String password);

    TokenPair refreshToken(String refreshToken);

    TokenPair getServiceToken(String clientId, String clientSecret, String scope);

    TokenValidationResult validateToken(String token);

    void assignRealmRoles(String userId, Set<String> roles);

    void syncUserAttributes(String userId, Map<String, Object> attributes);

    void initiatePasswordReset(String email);

    void changePassword(String accessToken, String currentPassword, String newPassword);
}
