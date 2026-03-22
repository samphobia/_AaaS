package com.authservice.infrastructure.security;

import com.authservice.application.exception.UnauthorizedException;

import java.util.UUID;

public final class TenantContextHolder {

    private static final ThreadLocal<UUID> TENANT_ID = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void setTenantId(UUID tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static UUID getRequiredTenantId() {
        UUID tenantId = TENANT_ID.get();
        if (tenantId == null) {
            throw new UnauthorizedException("Tenant context not resolved");
        }
        return tenantId;
    }

    public static void clear() {
        TENANT_ID.remove();
    }
}
