package com.authservice.infrastructure.persistence.repository;

import com.authservice.domain.model.Tenant;
import com.authservice.domain.repository.TenantRepository;
import com.authservice.infrastructure.persistence.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TenantRepositoryAdapter implements TenantRepository {

    private final SpringDataTenantRepository springDataTenantRepository;

    @Override
    public Optional<Tenant> findByApiKey(String apiKey) {
        return springDataTenantRepository.findByApiKey(apiKey)
                .map(TenantMapper::toDomain);
    }
}
