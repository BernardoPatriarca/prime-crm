-- Templates de comunicacao (e-mail, proposta, contrato, whatsapp).
CREATE TABLE templates (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    type         VARCHAR(20) NOT NULL CHECK (type IN ('EMAIL', 'PROPOSAL', 'CONTRACT', 'WHATSAPP')),
    name         VARCHAR(150) NOT NULL,
    subject      VARCHAR(255),
    content      TEXT NOT NULL,
    active       BOOLEAN NOT NULL DEFAULT true,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by   VARCHAR(120),
    updated_by   VARCHAR(120),
    deleted_at   TIMESTAMPTZ
);

CREATE INDEX idx_templates_tenant_id ON templates (tenant_id);
CREATE INDEX idx_templates_type ON templates (type);
CREATE INDEX idx_templates_not_deleted ON templates (id) WHERE deleted_at IS NULL;
