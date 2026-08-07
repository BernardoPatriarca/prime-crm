-- Engine generico de parametrizacao: domain_types (catalogo tecnico fixo, global)
-- e domain_values (valores por tenant, com soft delete e auditoria via BaseEntity).

CREATE TABLE domain_types (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code             VARCHAR(60) NOT NULL,
    label            VARCHAR(120) NOT NULL,
    supports_color   BOOLEAN NOT NULL DEFAULT true,
    supports_icon    BOOLEAN NOT NULL DEFAULT true,
    system_defined   BOOLEAN NOT NULL DEFAULT true,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_domain_types_code UNIQUE (code)
);

CREATE TABLE domain_values (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    domain_type_id   UUID NOT NULL REFERENCES domain_types (id),
    code             VARCHAR(80),
    name             VARCHAR(150) NOT NULL,
    description      TEXT,
    color            VARCHAR(7),
    icon             VARCHAR(60),
    display_order    INT NOT NULL DEFAULT 0,
    extra            JSONB,
    active           BOOLEAN NOT NULL DEFAULT true,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by       VARCHAR(120),
    updated_by       VARCHAR(120),
    deleted_at       TIMESTAMPTZ
);

CREATE INDEX idx_domain_values_tenant_id ON domain_values (tenant_id);
CREATE INDEX idx_domain_values_domain_type_id ON domain_values (domain_type_id);
CREATE INDEX idx_domain_values_type_tenant_active ON domain_values (domain_type_id, tenant_id, active);
CREATE INDEX idx_domain_values_not_deleted ON domain_values (id) WHERE deleted_at IS NULL;

-- Codigo estavel (slug) e unico por tipo+tenant quando informado.
CREATE UNIQUE INDEX uq_domain_values_type_tenant_code
    ON domain_values (domain_type_id, tenant_id, code)
    WHERE code IS NOT NULL;
