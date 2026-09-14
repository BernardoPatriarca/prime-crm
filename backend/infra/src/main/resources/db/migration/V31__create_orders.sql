-- Fase 4 - Pedidos: confirmacao formal de uma venda, tipicamente originada de uma Proposta
-- aceita (mas pode ser criado direto tambem). Mesma estrutura cabecalho+itens de Propostas.
CREATE SEQUENCE order_code_seq START 1000;

CREATE TABLE orders (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    code              VARCHAR(30) DEFAULT ('PED-' || lpad(nextval('order_code_seq')::text, 6, '0')),
    customer_id       UUID NOT NULL REFERENCES customers (id),
    proposal_id       UUID REFERENCES proposals (id),
    opportunity_id    UUID REFERENCES opportunities (id),
    owner_user_id     UUID REFERENCES users (id),
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                          CHECK (status IN ('PENDING', 'CONFIRMED', 'DELIVERED', 'CANCELED')),
    order_date        DATE NOT NULL DEFAULT CURRENT_DATE,
    delivery_date     DATE,
    notes             TEXT,
    total_amount      NUMERIC(15, 2) NOT NULL DEFAULT 0,
    closed_at         TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        VARCHAR(120),
    updated_by        VARCHAR(120),
    deleted_at        TIMESTAMPTZ
);

CREATE INDEX idx_orders_tenant_id ON orders (tenant_id);
CREATE INDEX idx_orders_customer_id ON orders (customer_id);
CREATE INDEX idx_orders_proposal_id ON orders (proposal_id);
CREATE INDEX idx_orders_opportunity_id ON orders (opportunity_id);
CREATE INDEX idx_orders_owner_user_id ON orders (owner_user_id);
CREATE INDEX idx_orders_not_deleted ON orders (id) WHERE deleted_at IS NULL;
CREATE INDEX idx_orders_tenant_status ON orders (tenant_id, status) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_orders_tenant_code
    ON orders (tenant_id, code)
    WHERE code IS NOT NULL AND deleted_at IS NULL;
