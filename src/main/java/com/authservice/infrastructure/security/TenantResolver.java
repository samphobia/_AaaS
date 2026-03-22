package com.authservice.infrastructure.security;

import com.authservice.domain.model.Tenant;

public interface TenantResolver {

    Tenant resolve(String apiKey);
}
