-- Fase 4 - Propostas: documento comercial enviado ao cliente, com itens de produto/servico.
-- Segue o mesmo padrao de cabecalho+itens ja usado em pipelines/pipeline_stages: o item e um
-- recurso proprio (proposal_items), aninhado ao id da proposta, nao um array dentro do request.
CREATE SEQUENCE proposal_code_seq START 1000;

CREATE TABLE proposals (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    code              VARCHAR(30) DEFAULT ('PRO-' || lpad(nextval('proposal_code_seq')::text, 6, '0')),
    customer_id       UUID NOT NULL REFERENCES customers (id),
    contact_id        UUID REFERENCES contacts (id),
    opportunity_id    UUID REFERENCES opportunities (id),
    owner_user_id     UUID REFERENCES users (id),
    status            VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                          CHECK (status IN ('DRAFT', 'SENT', 'ACCEPTED', 'REJECTED')),
    issue_date        DATE NOT NULL DEFAULT CURRENT_DATE,
    valid_until       DATE,
    notes             TEXT,
    total_amount      NUMERIC(15, 2) NOT NULL DEFAULT 0,
    decided_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        VARCHAR(120),
    updated_by        VARCHAR(120),
    deleted_at        TIMESTAMPTZ
);

CREATE INDEX idx_proposals_tenant_id ON proposals (tenant_id);
CREATE INDEX idx_proposals_customer_id ON proposals (customer_id);
CREATE INDEX idx_proposals_opportunity_id ON proposals (opportunity_id);
CREATE INDEX idx_proposals_owner_user_id ON proposals (owner_user_id);
CREATE INDEX idx_proposals_not_deleted ON proposals (id) WHERE deleted_at IS NULL;
CREATE INDEX idx_proposals_tenant_status ON proposals (tenant_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_proposals_valid_until ON proposals (valid_until) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_proposals_tenant_code
    ON proposals (tenant_id, code)
    WHERE code IS NOT NULL AND deleted_at IS NULL;
