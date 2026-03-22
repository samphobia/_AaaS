package com.authservice.infrastructure.persistence.mapper;

import com.authservice.domain.model.AuthUser;
import com.authservice.domain.model.Role;
import com.authservice.infrastructure.persistence.entity.AuthUserEntity;
import com.authservice.infrastructure.persistence.entity.TenantEntity;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class AuthUserMapper {

    private AuthUserMapper() {
    }

    public static AuthUser toDomain(AuthUserEntity entity) {
        return AuthUser.builder()
                .id(entity.getId())
                .keycloakUserId(entity.getKeycloakUserId())
                .externalUserId(entity.getExternalUserId())
                .roles(parseRoles(entity.getRoles()))
                .tenantId(entity.getTenant().getId())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static AuthUserEntity toEntity(AuthUser model) {
        return AuthUserEntity.builder()
                .id(model.getId())
                .keycloakUserId(model.getKeycloakUserId())
                .externalUserId(model.getExternalUserId())
                .roles(formatRoles(model.getRoles()))
                .tenant(TenantEntity.builder().id(model.getTenantId()).build())
                .active(model.isActive())
                .createdAt(model.getCreatedAt())
                .build();
    }

    private static Set<Role> parseRoles(String roles) {
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Role::valueOf)
                .collect(Collectors.toSet());
    }

    private static String formatRoles(Set<Role> roles) {
        return roles.stream().map(Enum::name).collect(Collectors.joining(","));
    }
}
