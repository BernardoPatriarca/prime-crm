-- Configuracoes gerais por tenant (chave/valor).
CREATE TABLE system_settings (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    setting_key    VARCHAR(100) NOT NULL,
    setting_value  TEXT,
    description    VARCHAR(255),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by     VARCHAR(120),
    updated_by     VARCHAR(120),
    deleted_at     TIMESTAMPTZ
);

CREATE INDEX idx_system_settings_tenant_id ON system_settings (tenant_id);
CREATE INDEX idx_system_settings_not_deleted ON system_settings (id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_system_settings_tenant_key
    ON system_settings (tenant_id, setting_key)
    WHERE deleted_at IS NULL;
