INSERT INTO tenants (id, name, api_key)
VALUES ('11111111-1111-1111-1111-111111111111', 'Default Tenant', 'dev-default-api-key')
ON CONFLICT (api_key) DO NOTHING;
