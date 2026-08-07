-- Metadados de campos personalizados por entidade de negocio (preparado para a Fase 2,
-- quando entidades como CLIENTE/LEAD passarem a existir).
CREATE TABLE custom_fields (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    target_entity  VARCHAR(60) NOT NULL,
    field_key      VARCHAR(100) NOT NULL,
    label          VARCHAR(150) NOT NULL,
    field_type     VARCHAR(20) NOT NULL
                      CHECK (field_type IN ('TEXT', 'NUMBER', 'DATE', 'SELECT', 'MULTISELECT', 'BOOLEAN')),
    options        JSONB,
    required       BOOLEAN NOT NULL DEFAULT false,
    display_order  INT NOT NULL DEFAULT 0,
    active         BOOLEAN NOT NULL DEFAULT true,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by     VARCHAR(120),
    updated_by     VARCHAR(120),
    deleted_at     TIMESTAMPTZ
);

CREATE INDEX idx_custom_fields_tenant_id ON custom_fields (tenant_id);
CREATE INDEX idx_custom_fields_target_entity ON custom_fields (target_entity);
CREATE INDEX idx_custom_fields_not_deleted ON custom_fields (id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_custom_fields_tenant_entity_key
    ON custom_fields (tenant_id, target_entity, field_key)
    WHERE deleted_at IS NULL;
