-- Log de auditoria de alteracoes. user_id sem cascade (ON DELETE SET NULL) pois o
-- registro deve sobreviver a remocao do usuario; user_email guarda o snapshot.
CREATE TABLE audit_log (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_name  VARCHAR(120) NOT NULL,
    entity_id    UUID,
    action       VARCHAR(20) NOT NULL CHECK (action IN ('CREATE', 'UPDATE', 'DELETE')),
    changes      JSONB,
    user_id      UUID REFERENCES users (id) ON DELETE SET NULL,
    user_email   VARCHAR(180),
    ip_address   VARCHAR(64),
    user_agent   VARCHAR(255),
    tenant_id    UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_entity ON audit_log (entity_name, entity_id);
CREATE INDEX idx_audit_log_created_at ON audit_log (created_at);
CREATE INDEX idx_audit_log_tenant_id ON audit_log (tenant_id);
