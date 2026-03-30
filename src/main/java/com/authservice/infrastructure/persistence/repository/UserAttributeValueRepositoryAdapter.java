package com.authservice.infrastructure.persistence.repository;

import com.authservice.domain.model.UserAttributeValue;
import com.authservice.domain.repository.UserAttributeValueRepository;
import com.authservice.infrastructure.persistence.mapper.UserAttributeValueMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserAttributeValueRepositoryAdapter implements UserAttributeValueRepository {

    private final SpringDataUserAttributeValueRepository springDataUserAttributeValueRepository;

    @Override
    public List<UserAttributeValue> findByUserId(UUID userId) {
        return springDataUserAttributeValueRepository.findByUser_Id(userId).stream()
                .map(UserAttributeValueMapper::toDomain)
                .toList();
    }

    @Override
    public List<UserAttributeValue> saveAll(Collection<UserAttributeValue> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        return springDataUserAttributeValueRepository.saveAll(
                        values.stream().map(UserAttributeValueMapper::toEntity).toList()
                ).stream()
                .map(UserAttributeValueMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteAll(Collection<UserAttributeValue> values) {
        if (values == null || values.isEmpty()) {
            return;
        }

        springDataUserAttributeValueRepository.deleteAll(
                values.stream().map(UserAttributeValueMapper::toEntity).toList()
        );
    }
}
