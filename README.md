# KeyAuth (Auth as a Service)

## Current System Flow (Including Custom User Attributes)

### 1) Request enters filter chain
- `TenantFilter` resolves tenant context first.
- In `MULTI_TENANT` mode, `X-API-KEY` must map to a tenant.
- In `STANDALONE` mode, resolver always uses `auth.default-tenant-api-key`.
- Tenant ID is stored in `TenantContextHolder` for the request lifecycle.

### 2) JWT auth is resolved
- `JwtAuthenticationFilter` validates bearer token through the identity provider client.
- Authorities are built from both Keycloak roles (`ROLE_*`) and scopes (`SCOPE_*`).
- Role/scopes extraction merges token introspection fields and JWT claims to remain compatible with Keycloak claim variations.
- Principal resolution fallback order:
  1. token validation `subject`
  2. JWT `sub` claim
  3. token validation `username`
  4. token validation `clientId`
- For machine tokens, principal is forced to `clientId` when present.

### 3) Controller/application handling
- `POST /auth/register`: creates Keycloak user, tenant-scoped auth mapping, and validates/persists tenant-defined custom attributes during signup.
- `POST /auth/login`: returns access/refresh tokens.
- `POST /auth/refresh`: refreshes tokens.
- `POST /auth/forgot-password`: triggers Keycloak password reset action email for the provided email.
- `POST /auth/change-password`: authenticated password change using current password validation.
- `POST /auth/service-token`: client-credentials flow for service accounts.
- `GET /auth/me`: loads current tenant-scoped user + returns custom attributes map.
- `GET /auth/service/me`: returns machine principal and granted scopes.

## Custom User Attributes Flow

### Tenant onboarding with schema (new)
- Endpoint: `POST /tenants`
- Purpose: create tenant and provision its initial user attribute schema in one request.
- Outcome: tenant is immediately signup-ready; `POST /auth/register` enforces the provided schema.

Example:

```json
{
  "name": "Tenant A",
  "apiKey": "tenant-a-key",
  "attributes": [
    { "name": "name", "type": "STRING", "required": true },
    { "name": "idcard", "type": "STRING", "required": true },
    { "name": "email", "type": "STRING", "required": true }
  ]
}
```

### A) Define tenant attribute schema
- Endpoint: `POST /tenants/attributes`
- Input: `name`, `type` (`STRING|NUMBER|BOOLEAN`), `required`.
- Definition names are normalized to lowercase.
- Uniqueness is tenant-scoped: `(tenant_id, name)`.

### B) Assign attributes to user
- Endpoint: `POST /users/{id}/attributes`
- Payload: `attributes` key/value map.
- Validation rules:
  - User must belong to current tenant.
  - Attribute name must exist in current tenant definitions.
  - Value must match type.
  - Required attributes must remain present and non-blank.
- Behavior:
  - Upsert changed attributes.
  - Delete removed optional attributes (when value is `null`).

### B2) Signup-time attributes (required path)
- `POST /auth/register` accepts an `attributes` map.
- The register flow now enforces tenant-required custom attributes immediately.
- If tenant schema marks attributes as required, signup fails unless those values are provided with valid types.
- This guarantees each tenant can enforce distinct signup payload requirements from day one.
- The recommended bootstrap path is creating tenant + schema via `POST /tenants` before first signup.
- After persistence, attributes are synchronized to Keycloak user attributes under `custom_attributes` (JSON string), enabling token-level claim projection.

### C) Read attributes
- `GET /auth/me` returns:
  - `externalUserId`
  - `tenantId`
  - `roles`
  - `attributes` (typed values)
- Lookup avoids N+1:
  - fetch user values
  - fetch definitions in batch by IDs and tenant
  - map to typed output

## Data Model for Custom Attributes

Flyway migration: `V4__custom_user_attributes.sql`

- `custom_attribute_definitions`
  - `id`, `tenant_id`, `name`, `type`, `required`
  - unique `(tenant_id, name)`
- `user_attribute_values`
  - `id`, `user_id`, `attribute_id`, `value`
  - unique `(user_id, attribute_id)`
- indexes:
  - `tenant_id`, `user_id`, `attribute_id`
  - optional `(attribute_id, value)` for search/filtering

## Auth Mode Behavior Summary

- `MULTI_TENANT`
  - tenant selected from incoming API key
  - API key required on tenant-scoped routes
- `STANDALONE`
  - single default tenant from config
  - API key header not required for tenant resolution
  - Swagger/OpenAPI is mode-aware and hides tenant-key requirement in standalone mode

## Keycloak Authorization Ownership

- Keycloak is now the source of truth for runtime authorization.
- On registration, users are assigned Keycloak realm role `USER`.
- `/auth/me` resolves roles from authenticated token authorities (`ROLE_*`) and falls back to stored mapping roles only when token roles are absent (compatibility mode).

## Access Token Custom Attribute Claim

- Realm/client configuration now includes a protocol mapper that projects user attribute `custom_attributes` to token claim `custom_attributes`.
- `custom_attributes` contains a JSON object string of tenant-defined user attributes.
- Claim is emitted to access token, ID token, and userinfo response.

## Documentation Rule

After every major implementation, update documentation in the same change set with:
1. user-visible behavior changes (endpoints/response/auth requirements)
2. data model/migration changes
3. flow or architecture impact
4. test coverage notes (what was validated)

This repository now follows that rule starting with this README baseline.

## Register Payload Contract

Example:

```json
{
  "email": "john.doe@acme.com",
  "password": "StrongPass123!",
  "externalUserId": "crm-10001",
  "attributes": {
    "name": "John Doe",
    "idcard": "ID-10001"
  }
}
```

Notes:
- `attributes` keys are matched against tenant attribute definitions (normalized to lowercase).
- Values are type-validated (`STRING`, `NUMBER`, `BOOLEAN`).
- Missing required attributes returns `400` and registration is rejected.
