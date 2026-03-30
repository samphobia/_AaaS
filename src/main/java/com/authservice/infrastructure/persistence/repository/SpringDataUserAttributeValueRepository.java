package com.authservice.infrastructure.persistence.repository;

import com.authservice.infrastructure.persistence.entity.UserAttributeValueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataUserAttributeValueRepository extends JpaRepository<UserAttributeValueEntity, UUID> {

    List<UserAttributeValueEntity> findByUser_Id(UUID userId);
}
