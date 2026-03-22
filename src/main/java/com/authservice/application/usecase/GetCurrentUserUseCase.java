package com.authservice.application.usecase;

import com.authservice.domain.model.AuthUser;

public interface GetCurrentUserUseCase {

    AuthUser getCurrentUser(String keycloakUserId);
}
