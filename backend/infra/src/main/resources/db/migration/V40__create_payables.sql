-- Fase 5 - Contas a Pagar: despesas e pagamentos a fornecedores. O "fornecedor" e a mesma
-- tabela customers (ja marcada por CLIENT_TYPE = FORNECEDOR desde a Fase 1) em vez de um
-- cadastro proprio - e o mesmo registro de parceiro de negocio, so que do lado do pagamento.
-- Reaproveita tambem categoria (CATEGORY) e forma de pagamento (PAYMENT_METHOD) ja existentes.
CREATE SEQUENCE payable_code_seq START 1000;

CREATE TABLE payables (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id            UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    code                 VARCHAR(30) DEFAULT ('PAG-' || lpad(nextval('payable_code_seq')::text, 6, '0')),
    supplier_id          UUID NOT NULL REFERENCES customers (id),
    category_id          UUID REFERENCES domain_values (id),
    description          VARCHAR(200) NOT NULL,
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

CREATE INDEX idx_payables_tenant_id ON payables (tenant_id);
CREATE INDEX idx_payables_supplier_id ON payables (supplier_id);
CREATE INDEX idx_payables_category_id ON payables (category_id);
CREATE INDEX idx_payables_payment_method_id ON payables (payment_method_id);
CREATE INDEX idx_payables_not_deleted ON payables (id) WHERE deleted_at IS NULL;
CREATE INDEX idx_payables_tenant_status ON payables (tenant_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_payables_due_date ON payables (due_date) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_payables_tenant_code
    ON payables (tenant_id, code)
    WHERE code IS NOT NULL AND deleted_at IS NULL;
