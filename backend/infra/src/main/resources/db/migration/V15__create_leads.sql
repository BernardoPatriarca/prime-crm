-- Fase 2 - Nucleo Comercial: leads (pre-qualificacao) e suas tags.
-- Um lead convertido aponta para o cliente gerado em converted_customer_id / converted_at.
CREATE TABLE leads (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id              UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    code                   VARCHAR(30),
    name                   VARCHAR(200) NOT NULL,
    company_name           VARCHAR(200),
    contact_name           VARCHAR(200),
    email                  VARCHAR(180),
    phone                  VARCHAR(30),
    mobile                 VARCHAR(30),
    origin_id              UUID REFERENCES domain_values (id),
    status_id              UUID REFERENCES domain_values (id),
    priority_id            UUID REFERENCES domain_values (id),
    campaign               VARCHAR(150),
    owner_user_id          UUID REFERENCES users (id),
    pipeline_id            UUID REFERENCES pipelines (id),
    stage_id               UUID REFERENCES pipeline_stages (id),
    probability            NUMERIC(5,2) CHECK (probability >= 0 AND probability <= 100),
    estimated_value        NUMERIC(15,2),
    expected_close_date    DATE,
    -- Lead scoring de 0 a 100.
    qualification_score    INT CHECK (qualification_score >= 0 AND qualification_score <= 100),
    converted_customer_id  UUID REFERENCES customers (id),
    converted_at           TIMESTAMPTZ,
    notes                  TEXT,
    active                 BOOLEAN NOT NULL DEFAULT true,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by             VARCHAR(120),
    updated_by             VARCHAR(120),
    deleted_at             TIMESTAMPTZ
);

CREATE INDEX idx_leads_tenant_id ON leads (tenant_id);
CREATE INDEX idx_leads_origin_id ON leads (origin_id);
CREATE INDEX idx_leads_status_id ON leads (status_id);
CREATE INDEX idx_leads_priority_id ON leads (priority_id);
CREATE INDEX idx_leads_owner_user_id ON leads (owner_user_id);
CREATE INDEX idx_leads_pipeline_id ON leads (pipeline_id);
CREATE INDEX idx_leads_stage_id ON leads (stage_id);
CREATE INDEX idx_leads_converted_customer_id ON leads (converted_customer_id);
CREATE INDEX idx_leads_not_deleted ON leads (id) WHERE deleted_at IS NULL;

-- Colunas usadas em ordenacao/filtro/busca das listagens e do kanban.
CREATE INDEX idx_leads_tenant_name ON leads (tenant_id, name) WHERE deleted_at IS NULL;
CREATE INDEX idx_leads_tenant_active ON leads (tenant_id, active) WHERE deleted_at IS NULL;
CREATE INDEX idx_leads_email ON leads (email) WHERE email IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_leads_expected_close_date ON leads (expected_close_date) WHERE deleted_at IS NULL;
CREATE INDEX idx_leads_pipeline_stage ON leads (pipeline_id, stage_id) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_leads_tenant_code
    ON leads (tenant_id, code)
    WHERE code IS NOT NULL AND deleted_at IS NULL;

-- Vinculo N:N entre lead e domain_values do tipo TAG.
CREATE TABLE lead_tags (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id          UUID NOT NULL REFERENCES leads (id) ON DELETE CASCADE,
    domain_value_id  UUID NOT NULL REFERENCES domain_values (id) ON DELETE CASCADE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_lead_tags UNIQUE (lead_id, domain_value_id)
);

CREATE INDEX idx_lead_tags_lead_id ON lead_tags (lead_id);
CREATE INDEX idx_lead_tags_domain_value_id ON lead_tags (domain_value_id);
