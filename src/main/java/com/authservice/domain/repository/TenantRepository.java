package com.authservice.domain.repository;

import com.authservice.domain.model.Tenant;

import java.util.Optional;

public interface TenantRepository {

    Optional<Tenant> findByApiKey(String apiKey);

    Tenant save(Tenant tenant);
}
