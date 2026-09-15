-- Fase 6 - Metas comerciais: valor de vendas (oportunidades ganhas) esperado por vendedor em um mes.
-- reference_month sempre normalizado para o primeiro dia do mes (ex.: 2026-09-01).
CREATE TABLE sales_goals (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    owner_user_id     UUID NOT NULL REFERENCES users (id),
    reference_month   DATE NOT NULL,
    target_amount     NUMERIC(15, 2) NOT NULL,
    notes             TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        VARCHAR(120),
    updated_by        VARCHAR(120),
    deleted_at        TIMESTAMPTZ,
    CHECK (target_amount > 0),
    CHECK (reference_month = date_trunc('month', reference_month)::date)
);

CREATE INDEX idx_sales_goals_tenant_id ON sales_goals (tenant_id);
CREATE INDEX idx_sales_goals_owner_user_id ON sales_goals (owner_user_id);
CREATE INDEX idx_sales_goals_not_deleted ON sales_goals (id) WHERE deleted_at IS NULL;
CREATE INDEX idx_sales_goals_reference_month ON sales_goals (reference_month) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_sales_goals_owner_month
    ON sales_goals (tenant_id, owner_user_id, reference_month)
    WHERE deleted_at IS NULL;
