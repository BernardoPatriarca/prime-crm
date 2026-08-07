-- Fase 2 - Nucleo Comercial: oportunidades (negocios em funil) e o historico
-- de movimentacao entre etapas, usado para metricas de tempo por etapa.
CREATE TABLE opportunities (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id            UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    code                 VARCHAR(30),
    title                VARCHAR(200) NOT NULL,
    customer_id          UUID NOT NULL REFERENCES customers (id),
    contact_id           UUID REFERENCES contacts (id),
    pipeline_id          UUID NOT NULL REFERENCES pipelines (id),
    stage_id             UUID NOT NULL REFERENCES pipeline_stages (id),
    amount               NUMERIC(15,2),
    probability          NUMERIC(5,2) CHECK (probability >= 0 AND probability <= 100),
    owner_user_id        UUID REFERENCES users (id),
    team_id              UUID REFERENCES domain_values (id),
    opened_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    expected_close_date  DATE,
    closed_at            TIMESTAMPTZ,
    outcome              VARCHAR(10) NOT NULL DEFAULT 'OPEN'
                             CHECK (outcome IN ('OPEN', 'WON', 'LOST')),
    win_reason_id        UUID REFERENCES domain_values (id),
    loss_reason_id       UUID REFERENCES domain_values (id),
    competitor           VARCHAR(200),
    source_lead_id       UUID REFERENCES leads (id),
    notes                TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by           VARCHAR(120),
    updated_by           VARCHAR(120),
    deleted_at           TIMESTAMPTZ
);

CREATE INDEX idx_opportunities_tenant_id ON opportunities (tenant_id);
CREATE INDEX idx_opportunities_customer_id ON opportunities (customer_id);
CREATE INDEX idx_opportunities_contact_id ON opportunities (contact_id);
CREATE INDEX idx_opportunities_pipeline_id ON opportunities (pipeline_id);
CREATE INDEX idx_opportunities_stage_id ON opportunities (stage_id);
CREATE INDEX idx_opportunities_owner_user_id ON opportunities (owner_user_id);
CREATE INDEX idx_opportunities_team_id ON opportunities (team_id);
CREATE INDEX idx_opportunities_win_reason_id ON opportunities (win_reason_id);
CREATE INDEX idx_opportunities_loss_reason_id ON opportunities (loss_reason_id);
CREATE INDEX idx_opportunities_source_lead_id ON opportunities (source_lead_id);
CREATE INDEX idx_opportunities_not_deleted ON opportunities (id) WHERE deleted_at IS NULL;

-- Colunas usadas em ordenacao/filtro/busca das listagens e do kanban de funil.
CREATE INDEX idx_opportunities_tenant_title ON opportunities (tenant_id, title) WHERE deleted_at IS NULL;
CREATE INDEX idx_opportunities_tenant_outcome ON opportunities (tenant_id, outcome) WHERE deleted_at IS NULL;
CREATE INDEX idx_opportunities_pipeline_stage ON opportunities (pipeline_id, stage_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_opportunities_expected_close_date ON opportunities (expected_close_date) WHERE deleted_at IS NULL;
CREATE INDEX idx_opportunities_opened_at ON opportunities (opened_at) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_opportunities_tenant_code
    ON opportunities (tenant_id, code)
    WHERE code IS NOT NULL AND deleted_at IS NULL;

-- Historico de movimentacao entre etapas. from_stage_id e nulo no primeiro registro
-- (criacao da oportunidade); days_in_previous_stage e calculado no momento da movimentacao.
CREATE TABLE opportunity_stage_history (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    opportunity_id           UUID NOT NULL REFERENCES opportunities (id) ON DELETE CASCADE,
    from_stage_id            UUID REFERENCES pipeline_stages (id),
    to_stage_id              UUID NOT NULL REFERENCES pipeline_stages (id),
    moved_by_user_id         UUID REFERENCES users (id),
    moved_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    days_in_previous_stage   INT,
    note                     TEXT,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by               VARCHAR(120),
    updated_by               VARCHAR(120),
    deleted_at               TIMESTAMPTZ
);

CREATE INDEX idx_opportunity_stage_history_tenant_id ON opportunity_stage_history (tenant_id);
CREATE INDEX idx_opportunity_stage_history_opportunity_id ON opportunity_stage_history (opportunity_id);
CREATE INDEX idx_opportunity_stage_history_from_stage_id ON opportunity_stage_history (from_stage_id);
CREATE INDEX idx_opportunity_stage_history_to_stage_id ON opportunity_stage_history (to_stage_id);
CREATE INDEX idx_opportunity_stage_history_moved_by_user_id ON opportunity_stage_history (moved_by_user_id);
CREATE INDEX idx_opportunity_stage_history_not_deleted ON opportunity_stage_history (id) WHERE deleted_at IS NULL;

CREATE INDEX idx_opportunity_stage_history_opportunity_moved_at
    ON opportunity_stage_history (opportunity_id, moved_at);
