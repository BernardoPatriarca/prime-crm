-- Fase 4 - Contratos: vigencia formal de uma venda recorrente ou continuada, tipicamente
-- originada de um Pedido confirmado/entregue (mas pode ser criado direto tambem). Sem itens
-- proprios - o detalhamento fica no Pedido de origem; o contrato so precisa do valor recorrente
-- e do ciclo de faturamento.
CREATE SEQUENCE contract_code_seq START 1000;

CREATE TABLE contracts (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id             UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    code                  VARCHAR(30) DEFAULT ('CTR-' || lpad(nextval('contract_code_seq')::text, 6, '0')),
    customer_id           UUID NOT NULL REFERENCES customers (id),
    order_id              UUID REFERENCES orders (id),
    opportunity_id        UUID REFERENCES opportunities (id),
    owner_user_id         UUID REFERENCES users (id),
    billing_cycle_id      UUID REFERENCES domain_values (id),
    status                VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                              CHECK (status IN ('DRAFT', 'ACTIVE', 'SUSPENDED', 'TERMINATED')),
    start_date            DATE NOT NULL DEFAULT CURRENT_DATE,
    end_date              DATE,
    auto_renew            BOOLEAN NOT NULL DEFAULT false,
    recurring_amount      NUMERIC(15, 2) NOT NULL DEFAULT 0,
    notes                 TEXT,
    terminated_at         TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by            VARCHAR(120),
    updated_by            VARCHAR(120),
    deleted_at            TIMESTAMPTZ,
    CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_contracts_tenant_id ON contracts (tenant_id);
CREATE INDEX idx_contracts_customer_id ON contracts (customer_id);
CREATE INDEX idx_contracts_order_id ON contracts (order_id);
CREATE INDEX idx_contracts_opportunity_id ON contracts (opportunity_id);
CREATE INDEX idx_contracts_owner_user_id ON contracts (owner_user_id);
CREATE INDEX idx_contracts_billing_cycle_id ON contracts (billing_cycle_id);
CREATE INDEX idx_contracts_not_deleted ON contracts (id) WHERE deleted_at IS NULL;
CREATE INDEX idx_contracts_tenant_status ON contracts (tenant_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_contracts_end_date ON contracts (end_date) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_contracts_tenant_code
    ON contracts (tenant_id, code)
    WHERE code IS NOT NULL AND deleted_at IS NULL;
