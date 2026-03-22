CREATE TABLE tenants (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    api_key VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE auth_users (
    id UUID PRIMARY KEY,
    keycloak_user_id VARCHAR(255) NOT NULL,
    external_user_id VARCHAR(255) NOT NULL,
    roles VARCHAR(255) NOT NULL,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (external_user_id, tenant_id)
);

CREATE TABLE auth_metadata (
    user_id UUID PRIMARY KEY REFERENCES auth_users(id),
    last_login_at TIMESTAMP WITH TIME ZONE,
    last_login_ip VARCHAR(128),
    device VARCHAR(255)
);

CREATE INDEX idx_auth_users_external_user_id ON auth_users (external_user_id);
CREATE INDEX idx_auth_users_keycloak_user_id ON auth_users (keycloak_user_id);
CREATE INDEX idx_auth_users_tenant_id ON auth_users (tenant_id);
CREATE INDEX idx_tenants_api_key ON tenants (api_key);
