CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    event_type VARCHAR(128) NOT NULL,
    outcome VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    principal VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    details VARCHAR(1024)
);

CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);
CREATE INDEX idx_audit_logs_tenant_id ON audit_logs (tenant_id);
