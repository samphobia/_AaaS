package com.authservice.domain.repository;

import com.authservice.domain.model.UserAttributeValue;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UserAttributeValueRepository {

    List<UserAttributeValue> findByUserId(UUID userId);

    List<UserAttributeValue> saveAll(Collection<UserAttributeValue> values);

    void deleteAll(Collection<UserAttributeValue> values);
}
