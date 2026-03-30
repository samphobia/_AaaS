CREATE TABLE custom_attribute_definitions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_attr_def_tenant_name UNIQUE (tenant_id, name)
);

CREATE TABLE user_attribute_values (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth_users(id),
    attribute_id UUID NOT NULL REFERENCES custom_attribute_definitions(id),
    value TEXT NOT NULL,
    CONSTRAINT uk_user_attr_value_user_attr UNIQUE (user_id, attribute_id)
);

CREATE INDEX idx_attr_def_tenant_id ON custom_attribute_definitions (tenant_id);
CREATE INDEX idx_user_attr_values_user_id ON user_attribute_values (user_id);
CREATE INDEX idx_user_attr_values_attribute_id ON user_attribute_values (attribute_id);

-- Optional optimization for attribute-based filtering/search by definition+value.
CREATE INDEX idx_user_attr_values_attribute_value ON user_attribute_values (attribute_id, value);
