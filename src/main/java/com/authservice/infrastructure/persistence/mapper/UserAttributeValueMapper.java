package com.authservice.infrastructure.persistence.mapper;

import com.authservice.domain.model.UserAttributeValue;
import com.authservice.infrastructure.persistence.entity.AuthUserEntity;
import com.authservice.infrastructure.persistence.entity.CustomAttributeDefinitionEntity;
import com.authservice.infrastructure.persistence.entity.UserAttributeValueEntity;

public final class UserAttributeValueMapper {

    private UserAttributeValueMapper() {
    }

    public static UserAttributeValue toDomain(UserAttributeValueEntity entity) {
        return UserAttributeValue.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .attributeId(entity.getAttribute().getId())
                .value(entity.getValue())
                .build();
    }

    public static UserAttributeValueEntity toEntity(UserAttributeValue model) {
        return UserAttributeValueEntity.builder()
                .id(model.getId())
                .user(AuthUserEntity.builder().id(model.getUserId()).build())
                .attribute(CustomAttributeDefinitionEntity.builder().id(model.getAttributeId()).build())
                .value(model.getValue())
                .build();
    }
}
