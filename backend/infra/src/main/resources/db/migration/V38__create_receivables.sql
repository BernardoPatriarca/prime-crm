-- Fase 5 - Contas a Receber: parcelas de pagamento, tipicamente originadas de um Pedido
-- (uma linha por parcela) ou de um Contrato (cobranca recorrente lancada manualmente).
CREATE SEQUENCE receivable_code_seq START 1000;

CREATE TABLE receivables (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id            UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    code                 VARCHAR(30) DEFAULT ('REC-' || lpad(nextval('receivable_code_seq')::text, 6, '0')),
    customer_id          UUID NOT NULL REFERENCES customers (id),
    order_id             UUID REFERENCES orders (id),
    contract_id          UUID REFERENCES contracts (id),
    description          VARCHAR(200),
    installment_number   INT NOT NULL DEFAULT 1,
    total_installments   INT NOT NULL DEFAULT 1,
    due_date             DATE NOT NULL,
    amount               NUMERIC(15, 2) NOT NULL,
    paid_amount          NUMERIC(15, 2) NOT NULL DEFAULT 0,
    paid_at              TIMESTAMPTZ,
    payment_method_id    UUID REFERENCES domain_values (id),
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                             CHECK (status IN ('PENDING', 'PAID', 'CANCELED')),
    notes                TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by           VARCHAR(120),
    updated_by           VARCHAR(120),
    deleted_at           TIMESTAMPTZ,
    CHECK (amount > 0),
    CHECK (paid_amount >= 0)
);

CREATE INDEX idx_receivables_tenant_id ON receivables (tenant_id);
CREATE INDEX idx_receivables_customer_id ON receivables (customer_id);
CREATE INDEX idx_receivables_order_id ON receivables (order_id);
CREATE INDEX idx_receivables_contract_id ON receivables (contract_id);
CREATE INDEX idx_receivables_payment_method_id ON receivables (payment_method_id);
CREATE INDEX idx_receivables_not_deleted ON receivables (id) WHERE deleted_at IS NULL;
CREATE INDEX idx_receivables_tenant_status ON receivables (tenant_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_receivables_due_date ON receivables (due_date) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_receivables_tenant_code
    ON receivables (tenant_id, code)
    WHERE code IS NOT NULL AND deleted_at IS NULL;
