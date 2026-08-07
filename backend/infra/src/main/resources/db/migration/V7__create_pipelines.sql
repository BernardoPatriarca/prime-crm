-- Funis (pipelines) e suas etapas.
CREATE TABLE pipelines (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    name           VARCHAR(150) NOT NULL,
    business_type  VARCHAR(100),
    active         BOOLEAN NOT NULL DEFAULT true,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by     VARCHAR(120),
    updated_by     VARCHAR(120),
    deleted_at     TIMESTAMPTZ
);

CREATE INDEX idx_pipelines_tenant_id ON pipelines (tenant_id);
CREATE INDEX idx_pipelines_not_deleted ON pipelines (id) WHERE deleted_at IS NULL;

CREATE TABLE pipeline_stages (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id              UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    pipeline_id            UUID NOT NULL REFERENCES pipelines (id) ON DELETE CASCADE,
    name                   VARCHAR(150) NOT NULL,
    display_order          INT NOT NULL DEFAULT 0,
    default_probability    NUMERIC(5,2) NOT NULL DEFAULT 0
                              CHECK (default_probability >= 0 AND default_probability <= 100),
    sla_days               INT,
    color                  VARCHAR(7),
    requires_loss_reason   BOOLEAN NOT NULL DEFAULT false,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by             VARCHAR(120),
    updated_by             VARCHAR(120),
    deleted_at             TIMESTAMPTZ
);

CREATE INDEX idx_pipeline_stages_tenant_id ON pipeline_stages (tenant_id);
CREATE INDEX idx_pipeline_stages_pipeline_id ON pipeline_stages (pipeline_id);
CREATE INDEX idx_pipeline_stages_not_deleted ON pipeline_stages (id) WHERE deleted_at IS NULL;
